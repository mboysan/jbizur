package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.common.util.RngUtil;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BizurRun {

    private static final Logger logger = LoggerFactory.getLogger(BizurRun.class);

    private final BizurNode node;

    private final String mapName;
    private final BucketContainer bucketContainer;
    final int contextId;

    BizurRun(BizurMap map) {
        this(map, IdUtil.generateId());
    }

    BizurRun(BizurMap map, int contextId) {
        this.node = map.node;
        this.mapName = map.mapName;
        this.bucketContainer = map.bucketContainer;
        this.contextId = contextId;
    }

    public int getContextId() {
        return contextId;
    }

    protected String logMsg(String msg) {
        return node.logMsg("map=" + mapName + ", msg=" + msg);
    }

    protected BizurSettings getSettings() {
        return node.getSettings();
    }

    protected <T> T route(MapRequest_NC command) throws BizurException {
        command.setMapName(mapName)
                .setContextId(contextId)
                .setCorrelationId(IdUtil.generateId());
        return node.route(command);
    }

    boolean publishAndWaitMajority(int correlationId, Supplier<NetworkCommand> cmdSupplier, Predicate<NetworkCommand> handler) {
        BooleanSupplier isMajorityAcked = node.subscribe(correlationId, handler);
        node.publish(cmdSupplier);
        return isMajorityAcked.getAsBoolean();
    }

    void send(NetworkCommand command) {
        try {
            node.send(command);
        } catch (IOException e) {
            logger.error(logMsg(e.getMessage()), e);
        }
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    void startElection(SerializableBucket localBucket) {
        int electId = localBucket.incrementAndGetElectId();

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> pleaseVote =
                () -> new PleaseVote_NC()
                        .setBucketIndex(localBucket.getIndex())
                        .setElectId(electId)
                        .setMapName(mapName)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, pleaseVote, (cmd) -> cmd instanceof AckVote_NC)) {
            localBucket.setLeaderAddress(getSettings().getAddress());
            localBucket.setLeader(true);

            logger.info(logMsg("elected as leader for index={}"), localBucket.getIndex());
        }
    }

    void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        int bucketIndex = pleaseVoteNc.getBucketIndex();
        int electId = pleaseVoteNc.getElectId();
        Address source = pleaseVoteNc.getSenderAddress();
        NetworkCommand vote;
        SerializableBucket localBucket = bucketContainer.tryAndLockBucket(bucketIndex, contextId);
        if (localBucket != null) {
            try {
                if (electId > localBucket.getVotedElectId()) {
                    localBucket.setVotedElectId(electId);
                    localBucket.setLeaderAddress(source);
                    localBucket.setLeader(source.equals(getSettings().getAddress()));

                    vote = new AckVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
                } else if (electId == localBucket.getVotedElectId() && source.equals(localBucket.getLeaderAddress())) {
                    vote = new AckVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
                } else {
                    vote = new NackVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
                }
            } finally {
                localBucket.unlock();
            }
        } else {
            vote = new NackVote_NC().setIndex(bucketIndex).ofRequest(pleaseVoteNc);
        }
        send(vote);
    }

    /* ***************************************************************************
     * Algorithm 2 - Bucket Replication: Write
     * ***************************************************************************/

    private boolean write(SerializableBucket localBucket) throws OperationFailedException {
        localBucket.incrementAndGetVerCounter();

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> replicaWrite =
                () -> new ReplicaWrite_NC()
                        .setBucketView(localBucket.createView())
                        .setMapName(mapName)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, replicaWrite, (r) -> r instanceof AckWrite_NC)) {
            return true;
        }

        localBucket.setLeaderAddress(null);
        localBucket.setLeader(false);
        throw new OperationFailedException(node.toString(), localBucket.getIndex());
    }

    void replicaWrite(ReplicaWrite_NC replicaWriteNc) {
        BucketView<Serializable, Serializable> recvBucketView = replicaWriteNc.getBucketView();
        int index = recvBucketView.getIndex();
        NetworkCommand response;
        SerializableBucket localBucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (localBucket != null) {
            try {
                //TODO: Proposal1
                /* Proposal1: Algorithm doesn't take received Bucket's full VER properties into account.
                   i.e. if ReplicaWrite of an older request is received, since bucket.ver.counter isn't taken
                   into account, we might end up replacing the latest bucket with an older one even though
                   the bucket.ver.electId >= votedElectId */
                if (recvBucketView.getVerElectId() > localBucket.getVotedElectId()
                        || localBucket.compareToView(recvBucketView) <= 0) {

                    localBucket.setVotedElectId(recvBucketView.getVerElectId());
                    localBucket.setBucketMap(recvBucketView.getBucketMap());
                    localBucket.setLeaderAddress(recvBucketView.getLeaderAddress());
                    localBucket.setLeader(recvBucketView.getLeaderAddress().equals(getSettings().getAddress()));

                    //TODO: Proposal2
                    /* When we send the view of the local bucket during the ensureRecovery phase with
                       the ReplicaRead command, we must send the latest VER of the bucket, otherwise
                       the ensureRecovery phase might end up using a bucket with an older VER. */
                    localBucket.setVerElectId(recvBucketView.getVerElectId());
                    localBucket.setVerCounter(recvBucketView.getVerCounter());

                    response = new AckWrite_NC().setIndex(index).ofRequest(replicaWriteNc);
                } else {
                    response = new NackWrite_NC().setIndex(index).ofRequest(replicaWriteNc);
                }
            } finally {
                localBucket.unlock();
            }
        } else {
            response = new NackWrite_NC().setIndex(index).ofRequest(replicaWriteNc);
        }
        send(response);
    }

    /* ***************************************************************************
     * Algorithm 3 - Bucket Replication: Read
     * ***************************************************************************/

    private void read(final SerializableBucket localBucket) throws IllegalLeaderOperationException, OperationFailedException {
        if (!localBucket.isLeader()) {
            throw new IllegalLeaderOperationException(node.toString(), localBucket.getIndex());
        }

        if (!ensureRecovery(localBucket)) {
            throw new OperationFailedException(node.toString(), localBucket.getIndex());
        }

        int correlationId = IdUtil.generateId();
        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(localBucket.getIndex())
                        .setElectId(localBucket.getElectId())
                        .setMapName(mapName)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, replicaRead, (r) -> r instanceof AckRead_NC)) {
            return;
        }

        localBucket.setLeaderAddress(null);
        localBucket.setLeader(false);
        throw new OperationFailedException(node.toString(), localBucket.getIndex());
    }

    void replicaRead(ReplicaRead_NC replicaReadNc) {
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();

        NetworkCommand resp;
        SerializableBucket localBucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (localBucket != null) {
            try {
                if (electId < localBucket.getVotedElectId()) {
                    resp = new NackRead_NC().setIndex(index).ofRequest(replicaReadNc);
                } else {
                    localBucket.setVotedElectId(electId);
                    localBucket.setLeaderAddress(source);
                    localBucket.setLeader(source.equals(getSettings().getAddress()));
                    resp = new AckRead_NC()
                            .setIndex(index)
                            .setBucketView(localBucket.createView())
                            .ofRequest(replicaReadNc);
                }
            } finally {
                localBucket.unlock();
            }

        } else {
            resp = new NackRead_NC().setIndex(index).ofRequest(replicaReadNc);
        }
        send(resp);
    }

    /* ***************************************************************************
     * Algorithm 4 - Bucket Replication: Recovery
     * ***************************************************************************/

    private boolean ensureRecovery(SerializableBucket localBucket) throws OperationFailedException {
        if (localBucket.getElectId() == localBucket.getVerElectId()) {
            return true;
        }

        logger.warn(logMsg("recovering index=" + localBucket.getIndex()));

        AtomicReference<BucketView<Serializable, Serializable>> maxVerBucketView = new AtomicReference<>(null);
        Predicate<NetworkCommand> handler = (cmd) -> {
            if (cmd instanceof AckRead_NC) {
                BucketView<Serializable, Serializable> bucketView = ((AckRead_NC) cmd).getBucketView();
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
                        .setIndex(localBucket.getIndex())
                        .setElectId(localBucket.getElectId())
                        .setMapName(mapName)
                        .setContextId(contextId)
                        .setCorrelationId(correlationId);

        if (publishAndWaitMajority(correlationId, replicaRead, handler)) {
            logger.info(logMsg("finalizing recovery with write, index={}"), localBucket.getIndex());
            BucketView<Serializable, Serializable> view = maxVerBucketView.get();
            localBucket.setVerElectId(localBucket.getElectId());
            localBucket.setVerCounter(0);
            localBucket.setBucketMap(view.getBucketMap());
            return write(localBucket);
        }

        localBucket.setLeaderAddress(null);
        localBucket.setLeader(false);
        throw new OperationFailedException(node.toString(), localBucket.getIndex());
    }


    /* ***************************************************************************
     * Algorithm 5 - Key-Value API
     * ***************************************************************************/

    private boolean isElectionNeeded(int index) {
        boolean isLeaderNull = false;
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (bucket != null) {
            try {
                isLeaderNull = bucket.getLeaderAddress() == null;
            } finally {
                bucket.unlock();
            }
        }
        if (isLeaderNull) {
            // if true, either no previous leader assigned to the leader or your leadership is abolished.
            return true;
        }
        // Otherwise some node was elected as the leader and we will just wait for synchronization.
        // Therefore, we fail 20% of the time to let other nodes try and handle the election process.
        // This will somewhat help prevent collisions, i.e. some node selecting itself as the leader
        // by disagreeing the majority vote.
        return RngUtil.nextInt(5) >= 2;
    }

    void _startElection(int index) {
        _startElection(index, true);
    }
    private void _startElection(int index, boolean mustWait) {
        if (isElectionNeeded(index)) {
            if (mustWait) {
                try {
                    int nodeIdx = node.getSettings().getRoleId() != null
                            ? IdUtil.hashKey(node.getSettings().getRoleId(), node.getSettings().getProcessCount())
                            : RngUtil.nextInt(node.getSettings().getProcessCount());
                    long sleepMs = RngUtil.nextInt(1000) * nodeIdx;
                    logger.info(logMsg("waiting " + sleepMs + " ms before starting election... index={}"), index);
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
            if (bucket != null) {
                try {
                    if (!isElectionNeeded(index)) {
                        // while we were waiting the leader might've already been updated.
                        logger.info(logMsg("election not needed exiting, index={}"), index);
                        return;
                    }
                    logger.info(logMsg("election needed proceeding, index={}"), index);
                    startElection(bucket);
                } finally {
                    bucket.unlock();
                }
            }
        }
    }

    private Serializable _get(Serializable key) throws IllegalLeaderOperationException, OperationFailedException {
        int index = bucketContainer.hashKey(key);
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (bucket != null) {
            try {
                read(bucket);
                return bucket.getOp(key);
            } finally {
                bucket.unlock();
            }
        }
        logger.warn(logMsg("retry get, k=" + key));
        return _get(key);
    }

    private Serializable _set(Serializable key, Serializable value) throws IllegalLeaderOperationException, OperationFailedException {
        int index = bucketContainer.hashKey(key);
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (bucket != null) {
            try {
                read(bucket);
                Serializable prevVal = bucket.putOp(key, value);
                if (write(bucket)) {
                    return prevVal;
                }
            } finally {
                bucket.unlock();
            }
        }
        logger.warn(logMsg("retry set, k=" + key + ", v=" + value));
        return _set(key, value);
    }

    private Serializable _delete(Serializable key) throws IllegalLeaderOperationException, OperationFailedException {
        int index = bucketContainer.hashKey(key);
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (bucket != null) {
            try {
                read(bucket);
                Serializable prevVal = bucket.removeOp(key);
                if (write(bucket)) {
                    return prevVal;
                }
            } finally {
                bucket.unlock();
            }
        }
        logger.warn(logMsg("retry delete, k=" + key));
        return _delete(key);
    }

    private Set<Serializable> _iterateKeys(int index) throws IllegalLeaderOperationException, OperationFailedException {
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (bucket != null) {
            try {
                read(bucket);
                return bucket.getKeySetOp();
            } finally {
                bucket.unlock();
            }
        }
        logger.warn(logMsg("retry iterate keys, index=" + index));
        return _iterateKeys(index);
    }




    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    Serializable get(Serializable key) {
        Objects.requireNonNull(key);
        int index = bucketContainer.hashKey(key);
        Address lead;
        try {
            lead = resolveLeader(index);
            return route((MapRequest_NC) new ApiGet_NC()
                            .setKey(key)
                            .setReceiverAddress(lead)
            );
        } catch (BizurException e) {
            logger.error(e.getMessage(), e);
            _startElection(index);
            return get(key);
        }
    }

    void getByLeader(ApiGet_NC getNc) {
        Serializable payload;
        try {
            payload = _get(getNc.getKey());
        } catch (IllegalLeaderOperationException | OperationFailedException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiGet_NC-[key=" + getNc.getKey() + "]")
                .setPayload(payload)
                .ofRequest(getNc)
        );
    }


    Serializable set(Serializable key, Serializable val) {
        Objects.requireNonNull(key);
        int index = bucketContainer.hashKey(key);
        Address lead;
        try {
            lead = resolveLeader(index);
            return route((MapRequest_NC) new ApiSet_NC()
                            .setKey(key)
                            .setVal(val)
                            .setReceiverAddress(lead)
            );
        } catch (BizurException e) {
            logger.error(e.getMessage(), e);
            _startElection(index);
            return set(key, val);
        }
    }

    void setByLeader(ApiSet_NC setNc) {
        Serializable payload;
        try {
            payload = _set(setNc.getKey(), setNc.getVal());
        } catch (IllegalLeaderOperationException | OperationFailedException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiSet_NC-[key=" + setNc.getKey() + ", val=" + setNc.getVal() + "]")
                .setPayload(payload)
                .ofRequest(setNc)
        );
    }

    Serializable delete(Serializable key) {
        Objects.requireNonNull(key);
        int index = bucketContainer.hashKey(key);
        Address lead;
        try {
            lead = resolveLeader(index);
            return route((MapRequest_NC) new ApiDelete_NC()
                            .setKey(key)
                            .setReceiverAddress(lead)
            );
        } catch (BizurException e) {
            logger.error(e.getMessage(), e);
            _startElection(index);
            return delete(key);
        }
    }

    void deleteByLeader(ApiDelete_NC deleteNc) {
        Serializable payload;
        try {
            payload = _delete(deleteNc.getKey());
        } catch (IllegalLeaderOperationException | OperationFailedException e) {
            logger.error(e.getMessage(), e);
            payload = e;
        }
        send(new LeaderResponse_NC()
                .setRequest("ApiDelete_NC-[key=" + deleteNc.getKey() + "]")
                .setPayload(payload)
                .ofRequest(deleteNc)
        );
    }

    Set<Serializable> iterateKeys() {
        Set<Serializable> keySet = new HashSet<>();
        Set<Integer> indices = bucketContainer.collectIndices();
        for (Integer index : indices) {
            keySet.addAll(iterateKeys(index));
        }
        return keySet;
    }

    private Set<Serializable> iterateKeys(int index) {
        Address lead;
        try {
            lead = resolveLeader(index);
            return route((MapRequest_NC) new ApiIterKeys_NC()
                            .setIndex(index)
                            .setReceiverAddress(lead)
            );
        } catch (BizurException e) {
            logger.error(e.getMessage(), e);
            _startElection(index);
            return iterateKeys(index);
        }
    }

    void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Serializable payload;
        try {
            payload = new HashSet<>(_iterateKeys(iterKeysNc.getIndex()));
        } catch (IllegalLeaderOperationException | OperationFailedException e) {
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
     * Leader resolution extras
     * ***************************************************************************/

    void startElection(int bucketIndex) {
        _startElection(bucketIndex, false);
    }

    Address resolveLeader(int index) {
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index, contextId);
        if (bucket != null) {
            try {
                Address lead = bucket.getLeaderAddress();
                if (lead != null) {
                    return lead;
                }
            } finally {
                bucket.unlock();
            }
        }
        _startElection(index);
        return resolveLeader(index);
    }

}
