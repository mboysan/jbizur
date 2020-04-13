package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.SendFail_IC;
import ee.ut.jbizur.protocol.commands.net.*;
import ee.ut.jbizur.common.util.IdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
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

    private static final int BUCKET_LEADER_ELECTION_RETRY_COUNT = CoreConf.get().consensus.bizur.bucketElectRetryCount;

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

    protected boolean ping(Address address) {
        try {
            return node.ping(address);
        } catch (IOException e) {
            logger.error(logMsg(e.getMessage()), e);
        }
        return false;
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

    private NetworkCommand sendRecv(NetworkCommand request) {
        try {
            return node.sendRecv(request);
        } catch (IOException e) {
            logger.error(logMsg(e.getMessage()), e);
        }
        return null;
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    protected void startElection(int bucketIndex) {
        Bucket localBucket = bucketContainer.getBucket(bucketIndex);
        int electId = localBucket.incrementAndGetElectId();

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> pleaseVote =
                () -> new PleaseVote_NC()
                        .setBucketIndex(bucketIndex)
                        .setElectId(electId)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, pleaseVote, (cmd) -> cmd instanceof Ack_NC)) {
            /* Note1: following is done to guarantee that in case the leader discards the PleaseVote request,
               the bucket leader is set properly for the first time. */
            localBucket.setElectId(electId);    // see Note2 below
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(getSettings().getAddress());

            localBucket.updateLeader(true);
        }
    }

    void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        int bucketIndex = pleaseVoteNc.getBucketIndex();
        int electId = pleaseVoteNc.getElectId();
        Address source = pleaseVoteNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getBucket(bucketIndex);

        NetworkCommand vote;
        if (electId > localBucket.getVotedElectId()) {
            /* Note2: even though the algorithm does not specify to update the electId, we need to do it
               because the replica needs to keep track of the latest election Id of the bucket. */
            localBucket.setElectId(electId);    // TODO: investigate if this introduces additional issues.

            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);
            localBucket.updateLeader(source.equals(getSettings().getAddress()));

            vote = new AckVote_NC().ofRequest(pleaseVoteNc);
        } else if (electId == localBucket.getVotedElectId() && source.equals(localBucket.getLeaderAddress())) {
            vote = new AckVote_NC().ofRequest(pleaseVoteNc);
        } else {
            vote = new NackVote_NC().ofRequest(pleaseVoteNc);
        }
        send(vote);
    }

    protected boolean initLeaderPerBucketElectionFlow() throws InterruptedException {
        for (int i = 0; i < bucketContainer.getNumBuckets(); i++) {
            Bucket localBucket = bucketContainer.getBucket(i);
            int retry = 0;
            int maxRetry = CoreConf.get().consensus.bizur.bucketElectRetryCount;
            while (retry < maxRetry) {
                electLeaderForBucket(localBucket, localBucket.getIndex(), false);
                if (localBucket.getLeaderAddress() != null) {
                    break;
                }
                retry++;
                logger.warn(logMsg("retrying (count=" + retry + ") leader election on bucket=[" + localBucket + "]"));
                Thread.sleep(500);
            }
            if (retry >= maxRetry) {
                return false;
            }
        }
        return true;
    }

    protected Address resolveLeader(Bucket bucket) {
        return resolveLeader(bucket, BUCKET_LEADER_ELECTION_RETRY_COUNT);
    }

    protected Address resolveLeader(Bucket bucket, int retry) {
        if (retry < 0) {
            throw new LeaderResolutionFailedException(logMsg("leader could not be resolved. Max retry count reached!"));
        }
        if (bucket.getLeaderAddress() != null) {
            return bucket.getLeaderAddress();
        } else {
            electLeaderForBucket(bucket, bucket.getIndex(), true);
            return resolveLeader(bucket, --retry);
        }
    }

    protected void electLeaderForBucket(Bucket localBucket, int startIdx, boolean forceElection) {
        Address nextAddr = IdUtil.nextAddressInUnorderedSet(getSettings().getMemberAddresses(), startIdx);
        if (nextAddr.equals(getSettings().getAddress())) {
            logger.info(logMsg("initializing election process on bucket idx=" + localBucket.getIndex()));
            electLeaderForBucket(localBucket);
        } else {
            boolean willHandle;
            if (forceElection) {
                willHandle = requestLeaderElectionForBucket(nextAddr, localBucket.getIndex());
            } else {
                willHandle = ping(nextAddr);
            }
            if (willHandle) {
                logger.info(logMsg("election process will be handled by: " + nextAddr + " for bucket idx=" + localBucket.getIndex()));
            } else {
                logger.warn(logMsg("address '" + nextAddr + "' unreachable, reinit election process for bucket idx=" + localBucket.getIndex() + " ..."));
                electLeaderForBucket(localBucket, startIdx + 1, forceElection);
            }
        }
    }

    protected Address electLeaderForBucket(Bucket localBucket) {
        int bucketIndex = localBucket.getIndex();
        bucketContainer.lockBucket(bucketIndex);
        try {
            startElection(bucketIndex);
        } finally {
            bucketContainer.unlockBucket(bucketIndex);
        }
        if (localBucket.getLeaderAddress() == null) {
            throw new IllegalStateException(logMsg("bucket has no leader: " + localBucket));
        }
        return localBucket.getLeaderAddress();
    }


    /* ***************************************************************************
     * Algorithm 2 - Bucket Replication: Write
     * ***************************************************************************/

    private boolean write(Bucket bucketToWrite) {
        Bucket localBucket = bucketContainer.getBucket(bucketToWrite.getIndex());
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
        localBucket.updateLeader(false);
        return false;
    }

    void replicaWrite(ReplicaWrite_NC replicaWriteNc) {
        BucketView recvBucketView = replicaWriteNc.getBucketView();
        Bucket localBucket = bucketContainer.getBucket(recvBucketView.getIndex());

        NetworkCommand response;
        if (recvBucketView.getVerElectId() < localBucket.getVotedElectId()) {
            response = new NackWrite_NC().ofRequest(replicaWriteNc);
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
        Bucket localBucket = bucketContainer.getBucket(index);
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

        localBucket.updateLeader(false);
        return null;
    }

    void replicaRead(ReplicaRead_NC replicaReadNc) {
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getBucket(index);

        NetworkCommand resp;
        if (electId < localBucket.getVotedElectId()) {
            resp = new NackRead_NC().ofRequest(replicaReadNc);
        } else {
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);
            localBucket.updateLeader(source.equals(getSettings().getAddress()));
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
        Bucket localBucket = bucketContainer.getBucket(index);
        if (electId == localBucket.getVerElectId()) {
            return true;
        }

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
            localBucket.updateLeader(false);
            isSuccess = false;
        }
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
            return false;
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    private Set<String> _iterateKeys() {
        Set<String> res = new HashSet<>();
        bucketContainer.bucketIndicesOfAddress(getSettings().getAddress()).forEach(bucketIdx -> {
            bucketContainer.lockBucket(bucketIdx);
            try {
                Bucket bucket = read(bucketIdx);
                if (bucket != null) {
                    res.addAll(bucket.getKeySetOp());
                } else {
                    logger.warn(logMsg(String.format("bucket keys could not be iterated by leader. bucketIdx=[%s]", bucketIdx)));
                }
            } finally {
                bucketContainer.unlockBucket(bucketIdx);
            }
        });
        return res;
    }



    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    String get(String key) {
        Bucket bucket = bucketContainer.getBucket(key);
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
            electLeaderForBucket(bucket, bucket.getIndex(), true);
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
        Bucket bucket = bucketContainer.getBucket(key);
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
            electLeaderForBucket(bucket, bucket.getIndex(), true);
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
        Bucket bucket = bucketContainer.getBucket(key);
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
            electLeaderForBucket(bucket, bucket.getIndex(), true);
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
        Set<Address> bucketLeaders = bucketContainer.collectAddressesWithBucketLeaders();
        bucketLeaders.forEach(leaderAddress -> {
            Set<String> keys = null;
            try {
                if (leaderAddress.equals(getSettings().getAddress())) {
                    // this is the leader so get keys from leader without routing
                    keys = _iterateKeys();
                } else {
                    NetworkCommand apiIterKeys = new ApiIterKeys_NC()
                            .setReceiverAddress(leaderAddress)
                            .setCorrelationId(IdUtil.generateId())
                            .setContextId(contextId);
                    keys = route(apiIterKeys);
                }
                if (keys == null) {
                    logger.warn(logMsg("Null keys received from leader: " + leaderAddress.toString()));
                }
            } catch (RoutingFailedException e) {
                logger.warn(logMsg("Operation failed while retrieving keys from leader: " + leaderAddress.toString()), e);
            }
            if (keys != null) {
                keySet.addAll(keys);
            } else {
                System.out.println();
            }
        });
        return keySet;
    }

    void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Set<String> keys = _iterateKeys();
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

    void handleLeaderElection(LeaderElectionRequest_NC ler) {
        Bucket localBucket = bucketContainer.getBucket(ler.getBucketIndex());

        boolean isSuccess;
        try {
            if (localBucket.checkLeaderElectionInProgress()) {
                localBucket.waitForLeaderElection();
                isSuccess = localBucket.getLeaderAddress().equals(getSettings().getAddress());
            } else {
                localBucket.initLeaderElection();
                try {
                    Address leaderAddr = electLeaderForBucket(localBucket);
                    isSuccess = leaderAddr.equals(getSettings().getAddress());
                } finally {
                    localBucket.endLeaderElection();
                }
            }
        } catch (Exception e) {
            isSuccess = false;
            logger.error(logMsg(e + ""), e);
        }

        send(new LeaderElectionResponse_NC()
                .setBucketIndex(ler.getBucketIndex())
                .setSuccess(isSuccess)
                .ofRequest(ler)
        );
    }

    private boolean requestLeaderElectionForBucket(Address address, int bucketIndex) {
        NetworkCommand request = new LeaderElectionRequest_NC()
                .setBucketIndex(bucketIndex)
                .setReceiverAddress(address)
                .setCorrelationId(IdUtil.generateId())
                .setContextId(contextId);

        NetworkCommand resp = sendRecv(request);
        if (resp instanceof LeaderElectionResponse_NC) {
            LeaderElectionResponse_NC ler = (LeaderElectionResponse_NC) resp;
            return ler.getBucketIndex() == bucketIndex && ler.isSuccess();
        }
        return false;
    }

    void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        node.handle(sendFailIc.getNackNC());
    }
}