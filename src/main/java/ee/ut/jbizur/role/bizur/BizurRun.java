package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.exceptions.LeaderResolutionFailedError;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.ICommand;
import ee.ut.jbizur.protocol.commands.ic.SendFail_IC;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.bizur.*;
import ee.ut.jbizur.protocol.commands.nc.common.Ack_NC;
import ee.ut.jbizur.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BizurRun {

    private static final Logger logger = LoggerFactory.getLogger(BizurRun.class);

    protected BizurNode node;

    protected final BucketContainer bucketContainer;
    private final int contextId;

    private static final int BUCKET_LEADER_ELECTION_RETRY_COUNT = Conf.get().consensus.bizur.bucketElectRetryCount;

    BizurRun(BizurNode node) {
        this(node, IdUtils.generateId());
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
        return node.ping(address);
    }

    protected <T> T route(NetworkCommand command) {
        return node.route(command);
    }

    private boolean publishAndWaitMajority(Supplier<NetworkCommand> cmdSupplier) {
        return publishAndWaitMajority(cmdSupplier, (c) -> {});
    }

    private boolean publishAndWaitMajority(Supplier<NetworkCommand> cmdSupplier, Consumer<NetworkCommand> cmdConsumer) {
        BooleanSupplier isMajorityAcked = node.subscribe(contextId, (cmd) -> {
            cmdConsumer.accept(cmd);
            return cmd instanceof Ack_NC;
        });
        node.publish(cmdSupplier);
        return isMajorityAcked.getAsBoolean();
    }

    private boolean publishAndWaitMajority(int contextId, Supplier<NetworkCommand> cmdSupplier, Predicate<NetworkCommand> handler) {
        BooleanSupplier isMajorityAcked = node.subscribe(contextId, handler);
        node.publish(cmdSupplier);
        return isMajorityAcked.getAsBoolean();
    }

    private void send(NetworkCommand command) {
        node.send(command);
    }

    private NetworkCommand sendRecv(NetworkCommand request) {
        return node.sendRecv(request);
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    protected void startElection(int bucketIndex) {
        Bucket localBucket = bucketContainer.getBucket(bucketIndex);
        int electId = localBucket.incrementAndGetElectId();

        Supplier<NetworkCommand> pleaseVote =
                () -> new PleaseVote_NC()
                        .setBucketIndex(bucketIndex)
                        .setElectId(electId)
                        .setCorrelationId(contextId);

//        int handlerId = IdUtils.generateId();
        int handlerId = contextId;
//        if (publishAndWaitMajority(pleaseVote)) {
        if (publishAndWaitMajority(handlerId, pleaseVote, (cmd) -> cmd instanceof Ack_NC)) {
            /* following is done to guarantee that in case the leader discards the PleaseVote request,
               the bucket leader is set properly for the first time. */
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
            int maxRetry = Conf.get().consensus.bizur.bucketElectRetryCount;
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
            throw new LeaderResolutionFailedError(logMsg("leader could not be resolved. Max retry count reached!"));
        }
        if (bucket.getLeaderAddress() != null) {
            return bucket.getLeaderAddress();
        } else {
            electLeaderForBucket(bucket, bucket.getIndex(), true);
            return resolveLeader(bucket, --retry);
        }
    }

    protected void electLeaderForBucket(Bucket localBucket, int startIdx, boolean forceElection) {
        Address nextAddr = IdUtils.nextAddressInUnorderedSet(getSettings().getMemberAddresses(), startIdx);
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

        Supplier<NetworkCommand> replicaWrite =
                () -> new ReplicaWrite_NC()
                        .setBucketView(bucketViewToSend);

        if (publishAndWaitMajority(replicaWrite)) {
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

        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId);

        if (publishAndWaitMajority(replicaRead)) {
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
        Consumer<NetworkCommand> handler = (cmd) -> {
            if (cmd instanceof AckRead_NC) {
                BucketView bucketView = ((AckRead_NC) cmd).getBucketView();
                synchronized (maxVerBucketView) {
                    if (!maxVerBucketView.compareAndSet(null, bucketView)) {
                        if (bucketView.compareTo(maxVerBucketView.get()) > 0) {
                            maxVerBucketView.set(bucketView);
                        }
                    }
                }
            }
        };

        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId);

        final boolean isSuccess;
        if (publishAndWaitMajority(replicaRead, handler)) {
            Bucket maxVerBucket = maxVerBucketView.get().createBucket();
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
                    logger.warn(logMsg(String.format("bucket keys could not be iterated by leader. bucket=[%s]", bucket)));
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
                            .setCorrelationId(contextId)
                            .setContextId(contextId)
            );
        } catch (OperationFailedError e) {
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
                .setCorrelationId(contextId)
                .setContextId(contextId)
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
                            .setCorrelationId(contextId)
                            .setContextId(contextId)
            );
        } catch (OperationFailedError e) {
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
                        .setCorrelationId(contextId)
                        .setContextId(contextId)
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
                            .setCorrelationId(contextId)
                            .setContextId(contextId)
            );
        } catch (OperationFailedError e) {
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
                        .setCorrelationId(contextId)
                        .setContextId(contextId)
        );
    }

    Set<String> iterateKeys() {
        Set<String> keySet = new HashSet<>();
        Set<Address> bucketLeaders = bucketContainer.collectAddressesWithBucketLeaders();
        bucketLeaders.forEach(leaderAddress -> {
            NetworkCommand apiIterKeys = new ApiIterKeys_NC()
                    .setReceiverAddress(leaderAddress)
                    .setCorrelationId(contextId)
                    .setContextId(contextId);
            Set<String> keys = null;
            try {
                if (leaderAddress.equals(getSettings().getAddress())) {
                    // this is the leader so get keys from leader without routing
                    keys = _iterateKeys();
                } else {
                    keys = route(apiIterKeys);
                }
                if (keys == null) {
                    logger.warn(logMsg("Null keys received from leader: " + leaderAddress.toString()));
                }
            } catch (OperationFailedError e) {
                logger.warn(logMsg("Operation failed while retrieving keys from leader: " + leaderAddress.toString()), e);
            }
            if (keys != null) {
                keySet.addAll(keys);
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
                        .setCorrelationId(contextId)
                        .setContextId(contextId)
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
                .setCorrelationId(contextId)
                .setContextId(contextId)
        );
    }

    private boolean requestLeaderElectionForBucket(Address address, int bucketIndex) {
        NetworkCommand request = new LeaderElectionRequest_NC()
                .setBucketIndex(bucketIndex)
                .setReceiverAddress(address)
                .setCorrelationId(contextId)
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
