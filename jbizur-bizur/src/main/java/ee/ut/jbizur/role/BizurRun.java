package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.SendFail_IC;
import ee.ut.jbizur.protocol.commands.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BizurRun {

    private static final Logger logger = LoggerFactory.getLogger(BizurRun.class);

    protected BizurNode node;

    protected final BucketContainer bucketContainer;
    private final int contextId;

    BizurRun(BizurNode node) {
        this(node, IdUtil.generateId());
    }

    BizurRun(BizurNode node, int contextId) {
        this.node = node;
        this.bucketContainer = node.bucketContainer;
        this.contextId = contextId;
    }

    public int getContextId() {
        return contextId;
    }

    protected String logMsg(String msg) {
        return node.logMsg(msg);
    }

    protected BizurSettings getSettings() {
        return node.getSettings();
    }

    protected <T> T route(NetworkCommand command) throws RoutingFailedException {
        return node.route(command);
    }

    private boolean publishAndWaitMajority(int correlationId, Supplier<NetworkCommand> cmdSupplier, Predicate<NetworkCommand> handler) {
        BooleanSupplier isMajorityAcked = node.subscribe(correlationId, handler);
        node.publish(cmdSupplier);
        return isMajorityAcked.getAsBoolean();
    }

    private void send(NetworkCommand command) {
        try {
            node.send(command);
        } catch (IOException e) {
            logger.error(logMsg(e.getMessage()), e);
        }
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    Address resolveLeader(Bucket bucket) {
        Address leader = bucket.getLeaderAddress();
        if (leader == null) {
            startElection(bucket.getIndex());
            return resolveLeader(bucket);
        }
        return leader;
    }

    protected void startElection(int bucketIndex) {
        Bucket localBucket = bucketContainer.getOrCreateBucket(bucketIndex);
        int electId = localBucket.incrementAndGetElectId();

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> pleaseVote =
                () -> new PleaseVote_NC()
                        .setBucketIndex(bucketIndex)
                        .setElectId(electId)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, pleaseVote, (cmd) -> cmd instanceof AckVote_NC)) {
            /* Note1: following is done to guarantee that in case the leader discards the PleaseVote request,
               the bucket leader is set properly for the first time. */
//            localBucket.setElectId(electId);    // see Note2 below
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(getSettings().getAddress());

            localBucket.setLeader(true);

            logger.info(logMsg("elected as leader for index={}"), bucketIndex);
        }
    }

    void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        int bucketIndex = pleaseVoteNc.getBucketIndex();
        int electId = pleaseVoteNc.getElectId();
        Address source = pleaseVoteNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getOrCreateBucket(bucketIndex);

        NetworkCommand vote;
        if (electId > localBucket.getVotedElectId()) {
            /* Note2: even though the algorithm does not specify to update the electId, we need to do it
               because the replica needs to keep track of the latest election Id of the bucket. */
//            localBucket.setElectId(electId);    // TODO: investigate if this introduces additional issues.

            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);
            localBucket.setLeader(source.equals(getSettings().getAddress()));

            vote = new AckVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
        } else if (electId == localBucket.getVotedElectId() && source.equals(localBucket.getLeaderAddress())) {
            vote = new AckVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
        } else {
            vote = new NackVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
        }
        send(vote);
    }

    /* ***************************************************************************
     * Algorithm 2 - Bucket Replication: Write
     * ***************************************************************************/

    private boolean write(Bucket bucketToWrite) {
        Bucket localBucket = bucketContainer.getOrCreateBucket(bucketToWrite.getIndex());
        bucketToWrite.setVerElectId(localBucket.getElectId());
        bucketToWrite.incrementAndGetVerCounter();
        BucketView bucketViewToSend = bucketToWrite.createView();

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> replicaWrite =
                () -> new ReplicaWrite_NC()
                        .setBucketView(bucketViewToSend)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, replicaWrite, (r) -> r instanceof AckWrite_NC)) {
            return true;
        }

        localBucket.setLeaderAddress(null);
        localBucket.setLeader(false);
        return false;
    }

    void replicaWrite(ReplicaWrite_NC replicaWriteNc) {
        BucketView recvBucketView = replicaWriteNc.getBucketView();
        Bucket localBucket = bucketContainer.getOrCreateBucket(recvBucketView.getIndex());

        NetworkCommand response;
        if (recvBucketView.getVerElectId() < localBucket.getVotedElectId()) {
            response = new NackWrite_NC().ofRequest(replicaWriteNc);
        } else if (localBucket.isLeader()) {
            /* Note3: we need to check the ver.counter, otherwise, if the leader tries to process
               an older ReplicaWrite command, the bucket will be replaced with an older version. */
            int verComparison = localBucket.compareToView(recvBucketView);
            if (verComparison > 0) {
                // local bucket has newer version
                response = new NackWrite_NC().ofRequest(replicaWriteNc);
            } else if (verComparison == 0) {
                // no need to re-write
                response = new AckWrite_NC().ofRequest(replicaWriteNc);
            } else {
                localBucket.replaceBucketForReplicationWith(recvBucketView);
                response = new AckWrite_NC().ofRequest(replicaWriteNc);
            }
        } else {
            localBucket.replaceBucketForReplicationWith(recvBucketView);
            response = new AckWrite_NC().ofRequest(replicaWriteNc);
        }
        send(response);
    }



    /* ***************************************************************************
     * Algorithm 3 - Bucket Replication: Read
     * ***************************************************************************/

    private Bucket read(int index) {
        Bucket localBucket = bucketContainer.getOrCreateBucket(index);
        int electId = localBucket.getElectId();
        if (!ensureRecovery(electId, index)) {
            return null;
        }

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, replicaRead, (r) -> r instanceof AckRead_NC)) {
            return localBucket;
        }

        localBucket.setLeaderAddress(null);
        localBucket.setLeader(false);
        return null;
    }

    void replicaRead(ReplicaRead_NC replicaReadNc) {
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getOrCreateBucket(index);

        NetworkCommand resp;
        if (electId < localBucket.getVotedElectId()) {
            resp = new NackRead_NC().ofRequest(replicaReadNc);
        } else {
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);
            localBucket.setLeader(source.equals(getSettings().getAddress()));
            resp = new AckRead_NC()
                    .setBucketView(localBucket.createView())
                    .ofRequest(replicaReadNc);
        }
        send(resp);
    }

    /* ***************************************************************************
     * Algorithm 4 - Bucket Replication: Recovery
     * ***************************************************************************/

    private boolean ensureRecovery(int electId, int index) {
        Bucket localBucket = bucketContainer.getOrCreateBucket(index);
        if (electId == localBucket.getVerElectId()) {
            return true;
        }
        logger.warn(logMsg("recovering index=" + index));

        AtomicReference<BucketView> maxVerBucketView = new AtomicReference<>(null);
        Predicate<NetworkCommand> handler = (cmd) -> {
            if (cmd instanceof AckRead_NC) {
                BucketView bucketView = ((AckRead_NC) cmd).getBucketView();
                synchronized (maxVerBucketView) {
                    if (!maxVerBucketView.compareAndSet(null, bucketView)) {
                        if (bucketView.compareTo(maxVerBucketView.get()) > 0) {
                            maxVerBucketView.set(bucketView);
                        }
                    }
                }
                return true;
            }
            return false;
        };

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        final boolean isSuccess;
        if (publishAndWaitMajority(correlationId, replicaRead, handler)) {
            Bucket maxVerBucket = Bucket.createBucket(maxVerBucketView.get());
            maxVerBucket.setVerElectId(electId);
            maxVerBucket.setVerCounter(0);
            isSuccess = write(maxVerBucket);
        } else {
            localBucket.setLeaderAddress(null);
            localBucket.setLeader(false);
            isSuccess = false;
        }
        logger.info(logMsg("index={} is recovered={}"), index, isSuccess);
        return isSuccess;
    }


    /* ***************************************************************************
     * Algorithm 5 - Key-Value API
     * ***************************************************************************/

    private String _get(String key) {
        int index = bucketContainer.hashKey(key);
        bucketContainer.lockBucket(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                return bucket.getOp(key);
            }
            logger.warn(logMsg(String.format("get failed. key=[%s]", key)));
            return null;
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    private boolean _set(String key, String value) {
        int index = bucketContainer.hashKey(key);
        bucketContainer.lockBucket(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                bucket.putOp(key, value);
                return write(bucket);
            }
            logger.warn(logMsg(String.format("set failed. key=[%s], value=[%s]", key, value)));
            return false;
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    private boolean _delete(String key) {
        int index = bucketContainer.hashKey(key);
        bucketContainer.lockBucket(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                bucket.removeOp(key);
                return write(bucket);
            }
            logger.warn(logMsg(String.format("delete failed. key=[%s]", key)));
            return false;
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    private Set<String> _iterateKeys(int bucketIdx) {
        bucketContainer.lockBucket(bucketIdx);
        try {
            Bucket bucket = read(bucketIdx);
            if (bucket != null) {
                return bucket.getKeySetOp();
            }
            logger.warn(logMsg(String.format("leader iterateKeys failed. bucketIdx=[%s]", bucketIdx)));
            return null;
        } finally {
            bucketContainer.unlockBucket(bucketIdx);
        }
    }



    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    String get(String key) {
        Objects.requireNonNull(key);
        Bucket bucket = bucketContainer.getOrCreateBucket(key);
        Address lead = resolveLeader(bucket);
        if (bucket.isLeader()) {
            return _get(key);
        }
        try {
            return route(
                    new ApiGet_NC()
                            .setKey(key)
                            .setReceiverAddress(lead)
                            .setCorrelationId(IdUtil.generateId())
                            .setContextId(contextId)
            );
        } catch (RoutingFailedException e) {
            logger.warn(e.getMessage(), e);
            startElection(bucket.getIndex());
            return get(key);
        }
    }

    void getByLeader(ApiGet_NC getNc) {
        String val = _get(getNc.getKey());
        send(new LeaderResponse_NC()
                .setRequest("ApiGet_NC-[key=" + getNc.getKey() + "]")
                .setPayload(val)
                .ofRequest(getNc)
        );
    }

    boolean set(String key, String val) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(val);
        Bucket bucket = bucketContainer.getOrCreateBucket(key);
        Address lead = resolveLeader(bucket);
        if (bucket.isLeader()) {
            return _set(key, val);
        }
        try {
            return route(
                    new ApiSet_NC()
                            .setKey(key)
                            .setVal(val)
                            .setReceiverAddress(lead)
                            .setCorrelationId(IdUtil.generateId())
                            .setContextId(contextId)
            );
        } catch (RoutingFailedException e) {
            logger.warn(e.getMessage(), e);
            startElection(bucket.getIndex());
            return set(key, val);
        }
    }

    void setByLeader(ApiSet_NC setNc) {
        boolean isSuccess = _set(setNc.getKey(), setNc.getVal());
        send(
                new LeaderResponse_NC()
                        .setRequest("ApiSet_NC-[key=" + setNc.getKey() + ", val=" + setNc.getVal() + "]")
                        .setPayload(isSuccess)
                        .ofRequest(setNc)
        );
    }

    boolean delete(String key) {
        Objects.requireNonNull(key);
        Bucket bucket = bucketContainer.getOrCreateBucket(key);
        Address lead = resolveLeader(bucket);
        if (bucket.isLeader()) {
            return _delete(key);
        }
        try {
            return route(
                    new ApiDelete_NC()
                            .setKey(key)
                            .setReceiverAddress(lead)
                            .setCorrelationId(IdUtil.generateId())
                            .setContextId(contextId)
            );
        } catch (RoutingFailedException e) {
            logger.warn(e.getMessage(), e);
            startElection(bucket.getIndex());
            return delete(key);
        }
    }

    void deleteByLeader(ApiDelete_NC deleteNc) {
        boolean isDeleted = _delete(deleteNc.getKey());
        send(
                new LeaderResponse_NC()
                        .setRequest("ApiDelete_NC-[key=" + deleteNc.getKey() + "]")
                        .setPayload(isDeleted)
                        .ofRequest(deleteNc)
        );
    }

    Set<String> iterateKeys() {
        Set<String> keySet = new HashSet<>();
        Set<Integer> indices = bucketContainer.collectIndices();
        for (Integer index : indices) {
            Bucket bucket = bucketContainer.getOrCreateBucket(index);
            keySet.addAll(iterateKeys(bucket));
        }
        return keySet;
    }

    private Set<String> iterateKeys(Bucket bucket) {
        int bucketIndex = bucket.getIndex();
        Address lead = resolveLeader(bucket);
        if (bucket.isLeader()) {
            return _iterateKeys(bucket.getIndex());
        }
        try {
            NetworkCommand apiIterKeys = new ApiIterKeys_NC()
                    .setIndex(bucketIndex)
                    .setReceiverAddress(lead)
                    .setCorrelationId(IdUtil.generateId())
                    .setContextId(contextId);
            return route(apiIterKeys);
        } catch (RoutingFailedException e) {
            logger.warn(e.getMessage(), e);
            startElection(bucket.getIndex());
            return iterateKeys(bucket);
        }
    }

    void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Set<String> keys = _iterateKeys(iterKeysNc.getIndex());
        send(
                new LeaderResponse_NC()
                        .setRequest("ApiIterKeys_NC")
                        .setPayload(keys)
                        .ofRequest(iterKeysNc)
        );
    }

    /* ***************************************************************************
     * Failure Handling
     * ***************************************************************************/

    void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        node.handle(sendFailIc.getNackNC());
    }
}
