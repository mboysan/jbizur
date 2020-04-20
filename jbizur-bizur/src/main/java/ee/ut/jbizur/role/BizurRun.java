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

    protected void startElection(int bucketIndex) {
        int electId;
        Bucket localBucket = bucketContainer.lockAndGetBucket(bucketIndex);
        try {
            /* if I start a new election, this means that I don't recognize any node as the leader */
            localBucket.setLeaderAddress(null);
            localBucket.setLeader(false);

            electId = localBucket.incrementAndGetElectId();
        } finally {
            bucketContainer.unlockBucket(bucketIndex);
        }

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> pleaseVote =
                () -> new PleaseVote_NC()
                        .setBucketIndex(bucketIndex)
                        .setElectId(electId)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, pleaseVote, (cmd) -> cmd instanceof AckVote_NC)) {
            localBucket = bucketContainer.lockAndGetBucket(bucketIndex);
            try {
                /* Note1: following is done to guarantee that in case the leader discards the PleaseVote request,
                   the bucket leader is set properly for the first time. */
//              localBucket.setElectId(electId);    // see Note2 below
//                localBucket.setVotedElectId(electId);
                localBucket.setLeaderAddress(getSettings().getAddress());
                localBucket.setLeader(true);

                logger.info(logMsg("elected as leader for index={}"), bucketIndex);
            } finally {
                bucketContainer.unlockBucket(bucketIndex);
            }
        }
    }

    void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        int bucketIndex = pleaseVoteNc.getBucketIndex();
        int electId = pleaseVoteNc.getElectId();
        Address source = pleaseVoteNc.getSenderAddress();
        NetworkCommand vote;
        Bucket localBucket = bucketContainer.lockAndGetBucket(bucketIndex);
        try {
            if (electId > localBucket.getVotedElectId()) {
                /* Note2: even though the algorithm does not specify to update the electId, we need to do it
                   because the replica needs to keep track of the latest election Id of the bucket. */
//              localBucket.setElectId(electId);    // TODO: investigate if this introduces additional issues.

                localBucket.setVotedElectId(electId);
                localBucket.setLeaderAddress(source);
//                localBucket.setLeader(source.equals(getSettings().getAddress()));

                vote = new AckVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
            } else if (electId == localBucket.getVotedElectId() && source.equals(localBucket.getLeaderAddress())) {
                vote = new AckVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
            } else {
                vote = new NackVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
            }
        } finally {
            bucketContainer.unlockBucket(bucketIndex);
        }
        send(vote);
    }

    /* ***************************************************************************
     * Algorithm 2 - Bucket Replication: Write
     * ***************************************************************************/

    private boolean write(Bucket bucketToWrite) throws IllegalLeaderOperationException {
        int index = bucketToWrite.getIndex();
        if (!isLeader(index)) {
//            throw new IllegalLeaderOperationException(node.toString(), index);
        }
        BucketView bucketViewToSend;
        Bucket localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            bucketToWrite.setVerElectId(localBucket.getElectId());
            bucketToWrite.incrementAndGetVerCounter();
            bucketViewToSend = bucketToWrite.createView();
        } finally {
            bucketContainer.unlockBucket(index);
        }

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> replicaWrite =
                () -> new ReplicaWrite_NC()
                        .setBucketView(bucketViewToSend)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, replicaWrite, (r) -> r instanceof AckWrite_NC)) {
            return true;
        }

        localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            localBucket.setLeaderAddress(null);
            localBucket.setLeader(false);
        } finally {
            bucketContainer.unlockBucket(index);
        }
//        return false;
        throw new IllegalLeaderOperationException(node.toString(), index);
    }

    void replicaWrite(ReplicaWrite_NC replicaWriteNc) {
        BucketView recvBucketView = replicaWriteNc.getBucketView();
        int index = recvBucketView.getIndex();
        NetworkCommand response;
        Bucket localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            if (isNotLeader(index, replicaWriteNc)) {
                // only leader can request this
                response = new NackWrite_NC().setIndex(index).ofRequest(replicaWriteNc);
            }
            else if (recvBucketView.getVerElectId() < localBucket.getVotedElectId()) {
                response = new NackWrite_NC().setIndex(index).ofRequest(replicaWriteNc);
            } else {
                localBucket.setVotedElectId(recvBucketView.getVerElectId());
                localBucket.setBucketMap(recvBucketView.getBucketMap());
                localBucket.setLeaderAddress(recvBucketView.getLeaderAddress());
//                localBucket.setLeader(recvBucketView.getLeaderAddress().equals(getSettings().getAddress()));

                response = new AckWrite_NC().setIndex(index).ofRequest(replicaWriteNc);
            }
        } finally {
            bucketContainer.unlockBucket(index);
        }
        send(response);
    }



    /* ***************************************************************************
     * Algorithm 3 - Bucket Replication: Read
     * ***************************************************************************/

    private Bucket read(int index) throws IllegalLeaderOperationException {
        if (!isLeader(index)) {
//            throw new IllegalLeaderOperationException(node.toString(), index);
        }

        int electId;
        Bucket localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            electId = localBucket.getElectId();
        } finally {
            bucketContainer.unlockBucket(index);
        }

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
            // bucket will be locked, the caller has the responsibility to unlock it
            return bucketContainer.lockAndGetBucket(index);
        }

        localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            localBucket.setLeaderAddress(null);
            localBucket.setLeader(false);
        } finally {
            bucketContainer.unlockBucket(index);
        }

        throw new IllegalLeaderOperationException(node.toString(), index);
//        return null;
    }

    void replicaRead(ReplicaRead_NC replicaReadNc) {
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();

        NetworkCommand resp;
        Bucket localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            if (isNotLeader(index, replicaReadNc)) {
                // only leader can request this
                resp = new NackRead_NC().setIndex(index).ofRequest(replicaReadNc);
            }
            else if (electId < localBucket.getVotedElectId()) {
                resp = new NackRead_NC().setIndex(index).ofRequest(replicaReadNc);
            } else {
                localBucket.setVotedElectId(electId);
                localBucket.setLeaderAddress(source);
//                localBucket.setLeader(source.equals(getSettings().getAddress()));
                resp = new AckRead_NC()
                        .setIndex(index)
                        .setBucketView(localBucket.createView())
                        .ofRequest(replicaReadNc);
            }
        } finally {
            bucketContainer.unlockBucket(index);
        }
        send(resp);
    }

    /* ***************************************************************************
     * Algorithm 4 - Bucket Replication: Recovery
     * ***************************************************************************/

    private boolean ensureRecovery(int electId, int index) throws IllegalLeaderOperationException {
        Bucket localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            if (electId == localBucket.getVerElectId()) {
                return true;
            }
        } finally {
            bucketContainer.unlockBucket(index);
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

        if (publishAndWaitMajority(correlationId, replicaRead, handler)) {
            // new bucket, i.e. no need to lock
            Bucket maxVerBucket = Bucket.createBucket(maxVerBucketView.get());
            maxVerBucket.setVerElectId(electId);
            maxVerBucket.setVerCounter(0);
            return write(maxVerBucket);
        } else {
            localBucket = bucketContainer.lockAndGetBucket(index);
            try {
                localBucket.setLeaderAddress(null);
                localBucket.setLeader(false);
            } finally {
                bucketContainer.unlockBucket(index);
            }
            throw new IllegalLeaderOperationException(node.toString(), index);
//            return false;
        }
    }


    /* ***************************************************************************
     * Algorithm 5 - Key-Value API
     * ***************************************************************************/

    private void _startElection(int index) {
        bucketContainer.apiLock(index);
        try {
            startElection(index);
        } finally {
            bucketContainer.apiUnlock(index);
        }
    }

    private String _get(String key) throws IllegalLeaderOperationException {
        int index = bucketContainer.hashKey(key);
        bucketContainer.apiLock(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                try {
                    return bucket.getOp(key);
                } finally {
                    bucketContainer.unlockBucket(index);
                }
            } else {
                logger.warn(logMsg(String.format("get failed. key=[%s]", key)));
//                return null;
                throw new IllegalLeaderOperationException(node.toString(), index);
            }
        } finally {
            bucketContainer.apiUnlock(index);
        }
    }

    private boolean _set(String key, String value) throws IllegalLeaderOperationException {
        int index = bucketContainer.hashKey(key);
        bucketContainer.apiLock(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                try {
                    bucket.putOp(key, value);
                } finally {
                    bucketContainer.unlockBucket(index);
                }
                return write(bucket);
            } else {
                logger.warn(logMsg(String.format("set failed. key=[%s], value=[%s]", key, value)));
//                return false;
                throw new IllegalLeaderOperationException(node.toString(), index);
            }
        } finally {
            bucketContainer.apiUnlock(index);
        }
    }

    private boolean _delete(String key) throws IllegalLeaderOperationException {
        int index = bucketContainer.hashKey(key);
        bucketContainer.apiLock(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                try {
                    bucket.removeOp(key);
                } finally {
                    bucketContainer.unlockBucket(index);
                }
                return write(bucket);
            } else {
                logger.warn(logMsg(String.format("delete failed. key=[%s]", key)));
//                return false;
                throw new IllegalLeaderOperationException(node.toString(), index);
            }
        } finally {
            bucketContainer.apiUnlock(index);
        }
    }

    private Set<String> _iterateKeys(int bucketIdx) throws IllegalLeaderOperationException {
        bucketContainer.apiLock(bucketIdx);
        try {
            Bucket bucket = read(bucketIdx);
            if (bucket != null) {
                try {
                    return bucket.getKeySetOp();
                } finally {
                    bucketContainer.unlockBucket(bucketIdx);
                }
            } else {
                logger.warn(logMsg(String.format("leader iterateKeys failed. bucketIdx=[%s]", bucketIdx)));
//                return null;
                throw new IllegalLeaderOperationException(node.toString(), bucketIdx);
            }
        } finally {
            bucketContainer.apiUnlock(bucketIdx);
        }
    }



    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    String get(String key) {
        Objects.requireNonNull(key);
        int index = bucketContainer.hashKey(key);
        Address lead;
        bucketContainer.apiLock(index);
        try {
            if (isLeader(index)) {
                try {
                    return _get(key);
                } catch (IllegalLeaderOperationException e) {
                    logger.warn(e.getMessage(), e);
                    _startElection(index);
                }
            }
            lead = resolveLeader(index);
        } finally {
            bucketContainer.apiUnlock(index);
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
            _startElection(index);
            return get(key);
        }
    }

    void getByLeader(ApiGet_NC getNc) {
        Object payload;
        try {
            payload = _get(getNc.getKey());
        } catch (IllegalLeaderOperationException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiGet_NC-[key=" + getNc.getKey() + "]")
                .setPayload(payload)
                .ofRequest(getNc)
        );
    }

    boolean set(String key, String val) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(val);
        int index = bucketContainer.hashKey(key);
        Address lead;
        bucketContainer.apiLock(index);
        try {
            if (isLeader(index)) {
                try {
                    return _set(key, val);
                } catch (IllegalLeaderOperationException e) {
                    logger.warn(e.getMessage(), e);
                    _startElection(index);
                }
            }
            lead = resolveLeader(index);
        } finally {
            bucketContainer.apiUnlock(index);
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
            _startElection(index);
            return set(key, val);
        }
    }

    void setByLeader(ApiSet_NC setNc) {
        Object payload;
        try {
            payload = _set(setNc.getKey(), setNc.getVal());
        } catch (IllegalLeaderOperationException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiSet_NC-[key=" + setNc.getKey() + ", val=" + setNc.getVal() + "]")
                .setPayload(payload)
                .ofRequest(setNc)
        );
    }

    boolean delete(String key) {
        Objects.requireNonNull(key);
        int index = bucketContainer.hashKey(key);
        Address lead;
        bucketContainer.apiLock(index);
        try {
            if (isLeader(index)) {
                try {
                    return _delete(key);
                } catch (IllegalLeaderOperationException e) {
                    logger.warn(e.getMessage(), e);
                    _startElection(index);
                }
            }
            lead = resolveLeader(index);
        } finally {
            bucketContainer.apiUnlock(index);
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
            _startElection(index);
            return delete(key);
        }
    }

    void deleteByLeader(ApiDelete_NC deleteNc) {
        Object payload;
        try {
            payload = _delete(deleteNc.getKey());
        } catch (IllegalLeaderOperationException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiDelete_NC-[key=" + deleteNc.getKey() + "]")
                .setPayload(payload)
                .ofRequest(deleteNc)
        );
    }

    Set<String> iterateKeys() {
        Set<String> keySet = new HashSet<>();
        Set<Integer> indices = bucketContainer.collectIndices();
        for (Integer index : indices) {
            keySet.addAll(iterateKeys(index));
        }
        return keySet;
    }

    private Set<String> iterateKeys(int index) {
        Address lead;
        bucketContainer.apiLock(index);
        try {
            if (isLeader(index)) {
                try {
                    return _iterateKeys(index);
                } catch (IllegalLeaderOperationException e) {
                    logger.warn(e.getMessage(), e);
                    _startElection(index);
                }
            }
            lead = resolveLeader(index);
        } finally {
            bucketContainer.apiUnlock(index);
        }
        try {
            return route(
                    new ApiIterKeys_NC()
                            .setIndex(index)
                            .setReceiverAddress(lead)
                            .setCorrelationId(IdUtil.generateId())
                            .setContextId(contextId)
            );
        } catch (RoutingFailedException e) {
            logger.warn(e.getMessage(), e);
            _startElection(index);
            return iterateKeys(index);
        }
    }

    void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Object payload;
        try {
            payload = _iterateKeys(iterKeysNc.getIndex());
        } catch (IllegalLeaderOperationException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiIterKeys_NC")
                .setPayload(payload)
                .ofRequest(iterKeysNc)
        );
    }

    /* ***************************************************************************
     * Leader resolution contd.
     * ***************************************************************************/

    Address resolveLeader(int index) {
        Address leader;
        bucketContainer.apiLock(index);
        try {
            Bucket bucket = bucketContainer.lockAndGetBucket(index);
            try {
                leader = bucket.getLeaderAddress();
            } finally {
                bucketContainer.unlockBucket(index);
            }
            if (leader == null) {
//                startElection(bucket.getIndex());
                _startElection(index);
                return resolveLeader(index);
            }
            return leader;
        } finally {
            bucketContainer.apiUnlock(index);
        }
    }

    boolean isLeader(int index) {
        bucketContainer.apiLock(index);
        try {
            Bucket bucket = bucketContainer.lockAndGetBucket(index);
            try {
                Address leader = bucket.getLeaderAddress();
                return bucket.isLeader()
                        && leader != null
                        && leader.equals(getSettings().getAddress());
            } finally {
                bucketContainer.unlockBucket(index);
            }
        } finally {
            bucketContainer.apiUnlock(index);
        }
    }

/*    private boolean isLeader(Bucket lockedBucket, NetworkCommand command) {
        if (!lockedBucket.isLocked()) {
            throw new IllegalStateException("bucket must be locked first, bucket=" + lockedBucket);
        }
        return lockedBucket.getLeaderAddress().equals(command.getSenderAddress());
    }*/

    private boolean isNotLeader(int index, NetworkCommand command) {
        return !isLeader(index, command);
    }

    private boolean isLeader(int index, NetworkCommand command) {
        Bucket localBucket = bucketContainer.lockAndGetBucket(index);
        try {
            Address lead = localBucket.getLeaderAddress();
            return lead != null && lead.equals(command.getSenderAddress());
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    Address _whoIsLeader(int index) {
        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> whoIsLeader =
                () -> new WhoIsLeaderRequest_NC()
                        .setIndex(index)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);
/*
        publishAndWaitMajority(correlationId, whoIsLeader, (cmd) -> {
            if (cmd instanceof WhoIsLeaderResponse_NC) {
                return ((WhoIsLeaderResponse_NC) cmd).getLeaderAddress();
            }
        })*/
        return null;
    }

    void whoIsLeader(WhoIsLeaderRequest_NC wilNc) {
        Integer index = wilNc.getIndex();
        NetworkCommand resp;
        Bucket bucket = bucketContainer.lockAndGetBucket(index);
        try {
            resp = new WhoIsLeaderResponse_NC()
                    .setIndex(index)
                    .setLeaderAddress(bucket.getLeaderAddress())
                    .ofRequest(wilNc);
        } finally {
            bucketContainer.unlockBucket(index);
        }
        send(resp);
    }

    /* ***************************************************************************
     * Failure Handling
     * ***************************************************************************/

    void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        node.handle(sendFailIc.getNackNC());
    }
}
