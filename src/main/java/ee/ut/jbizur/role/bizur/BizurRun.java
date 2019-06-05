package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.exceptions.LeaderResolutionFailedError;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.handlers.CallbackState;
import ee.ut.jbizur.network.handlers.QuorumState;
import ee.ut.jbizur.protocol.commands.ICommand;
import ee.ut.jbizur.protocol.commands.ic.SendFail_IC;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.bizur.*;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BizurRun implements AutoCloseable {

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
    protected void sendResponse(NetworkCommand command) {
        node.sendMessage(command);
    }

    protected CallbackState sendMsgWithCallback(NetworkCommand command, Predicate<ICommand> countdownHandler) {
        return node.sendMessage(command, null, countdownHandler);
    }

    private QuorumState sendMsgToAll(Supplier<NetworkCommand> commandTemplate) {
        return sendMsgToAll(commandTemplate, null, null);
    }

    private QuorumState sendMsgToAll(Supplier<NetworkCommand> commandTemplate, Predicate<ICommand> handler, Predicate<ICommand> countdownHandler) {
        return node.sendMessageToQuorum(commandTemplate, contextId, IdUtils.generateId(), handler, countdownHandler);
    }

    protected boolean pingAddress(Address address) {
        return node.pingAddress(address);
    }
    protected <T> T routeRequestAndGet(NetworkCommand command) {
        return node.routeRequestAndGet(command);
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
                        .setElectId(electId);

        if (sendMsgToAll(pleaseVote).awaitMajority()) {
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
        } else if(electId == localBucket.getVotedElectId() && source.equals(localBucket.getLeaderAddress())) {
            vote = new AckVote_NC().ofRequest(pleaseVoteNc);
        } else {
            vote = new NackVote_NC().ofRequest(pleaseVoteNc);
        }
        sendResponse(vote);
    }

    protected boolean initLeaderPerBucketElectionFlow() throws InterruptedException {
        for (int i = 0; i < bucketContainer.getNumBuckets(); i++) {
            Bucket localBucket = bucketContainer.getBucket(i);
            int retry = 0;
            int maxRetry = Conf.get().consensus.bizur.bucketElectRetryCount;
            while (retry < maxRetry) {
                if (localBucket.getLeaderAddress() == null) {
                    electLeaderForBucket(localBucket, localBucket.getIndex(), false);
                } else {
                    break;
                }
                retry++;
                Logger.warn(logMsg("retrying (count=" + retry + ") leader election on bucket=[" + localBucket + "]"));
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
            Logger.info(logMsg("initializing election process on bucket idx=" + localBucket.getIndex()));
            electLeaderForBucket(localBucket);
        } else {
            boolean willHandle;
            if (forceElection) {
                willHandle = requestLeaderElectionForBucket(nextAddr, localBucket.getIndex());
            } else {
                willHandle = pingAddress(nextAddr);
            }
            if (willHandle) {
                Logger.info(logMsg("election process will be handled by: " + nextAddr + " for bucket idx=" + localBucket.getIndex()));
            } else {
                Logger.warn(logMsg("address '" + nextAddr + "' unreachable, reinit election process for bucket idx=" + localBucket.getIndex() + " ..."));
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

        if (sendMsgToAll(replicaWrite).awaitMajority()) {
            return true;
        }
        localBucket.updateLeader(false);
        return false;
    }

    void replicaWrite(ReplicaWrite_NC replicaWriteNc) {
        BucketView recvBucketView = replicaWriteNc.getBucketView();
        Bucket localBucket = bucketContainer.getBucket(recvBucketView.getIndex());

        NetworkCommand response;
        if(recvBucketView.getVerElectId() < localBucket.getVotedElectId()){
            response = new NackWrite_NC().ofRequest(replicaWriteNc);
        } else {
            localBucket.replaceBucketForReplicationWith(recvBucketView);
            response = new AckWrite_NC().ofRequest(replicaWriteNc);
        }
        sendResponse(response);
    }



    /* ***************************************************************************
     * Algorithm 3 - Bucket Replication: Read
     * ***************************************************************************/

    private Bucket read(int index) {
        Bucket localBucket = bucketContainer.getBucket(index);
        int electId = localBucket.getElectId();
        if(!ensureRecovery(electId, index)){
            return null;
        }

        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId);

        if (sendMsgToAll(replicaRead).awaitMajority()) {
            return localBucket;
        }

        localBucket.updateLeader(false);
        return null;
    }

    void replicaRead(ReplicaRead_NC replicaReadNc){
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getBucket(index);

        NetworkCommand resp;
        if(electId < localBucket.getVotedElectId()){
            resp = new NackRead_NC().ofRequest(replicaReadNc);
        } else {
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);
            localBucket.updateLeader(source.equals(getSettings().getAddress()));
            resp = new AckRead_NC()
                    .setBucketView(localBucket.createView())
                    .ofRequest(replicaReadNc);
        }
        sendResponse(resp);
    }

    /* ***************************************************************************
     * Algorithm 4 - Bucket Replication: Recovery
     * ***************************************************************************/

    private boolean ensureRecovery(int electId, int index) {
        Bucket localBucket = bucketContainer.getBucket(index);
        if(electId == localBucket.getVerElectId()) {
            return true;
        }

        AtomicReference<BucketView> maxVerBucketView = new AtomicReference<>(null);
        Predicate<ICommand> handler = (cmd) -> {
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

        Supplier<NetworkCommand> replicaRead =
                () -> new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId);

        final boolean isSuccess;
        if (sendMsgToAll(replicaRead, handler, null).awaitMajority()) {
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
            if(bucket != null){
                return bucket.getOp(key);
            }
            return null;
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    private boolean _set(String key, String value){
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
            if(bucket != null){
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
                if(bucket != null){
                    res.addAll(bucket.getKeySetOp());
                } else {
                    Logger.warn(logMsg(String.format("bucket keys could not be iterated by leader. bucket=[%s]", bucket)));
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
            return routeRequestAndGet(
                    new ApiGet_NC()
                            .setKey(key)
                            .setReceiverAddress(lead)
                            .setContextId(contextId)
            );
        } catch (OperationFailedError e) {
            Logger.warn(e);
            electLeaderForBucket(bucket, bucket.getIndex(), true);
            return get(key);
        }
    }
    void getByLeader(ApiGet_NC getNc) {
        String val = _get(getNc.getKey());
        sendResponse(new LeaderResponse_NC()
                .setRequest("ApiGet_NC-[key=" + getNc.getKey() + "]")
                .setPayload(val)
                .ofRequest(getNc)
                .setContextId(contextId)
        );
    }

    boolean set(String key, String val) {
        Bucket bucket = bucketContainer.getBucket(key);
        Address lead = resolveLeader(bucket);
        if(bucket.isLeader()){
            return _set(key, val);
        }
        try {
            return routeRequestAndGet(
                    new ApiSet_NC()
                            .setKey(key)
                            .setVal(val)
                            .setReceiverAddress(lead)
                            .setContextId(contextId)
            );
        } catch (OperationFailedError e) {
            Logger.warn(e);
            electLeaderForBucket(bucket, bucket.getIndex(), true);
            return set(key, val);
        }
    }
    void setByLeader(ApiSet_NC setNc) {
        boolean isSuccess = _set(setNc.getKey(), setNc.getVal());
        sendResponse(
                new LeaderResponse_NC()
                        .setRequest("ApiSet_NC-[key=" + setNc.getKey() + ", val=" + setNc.getVal() + "]")
                        .setPayload(isSuccess)
                        .ofRequest(setNc)
                        .setContextId(contextId)
        );
    }

    boolean delete(String key) {
        Bucket bucket = bucketContainer.getBucket(key);
        Address lead = resolveLeader(bucket);
        if(bucket.isLeader()){
            return _delete(key);
        }
        try {
            return routeRequestAndGet(
                    new ApiDelete_NC()
                            .setKey(key)
                            .setReceiverAddress(lead)
                            .setContextId(contextId)
            );
        } catch (OperationFailedError e) {
            Logger.warn(e);
            electLeaderForBucket(bucket, bucket.getIndex(), true);
            return delete(key);
        }
    }
    void deleteByLeader(ApiDelete_NC deleteNc) {
        boolean isDeleted = _delete(deleteNc.getKey());
        sendResponse(
                new LeaderResponse_NC()
                        .setRequest("ApiDelete_NC-[key=" + deleteNc.getKey() + "]")
                        .setPayload(isDeleted)
                        .ofRequest(deleteNc)
                        .setContextId(contextId)
        );
    }

    Set<String> iterateKeys() {
        Set<String> keySet = new HashSet<>();
        Set<Address> bucketLeaders = bucketContainer.collectAddressesWithBucketLeaders();
        bucketLeaders.forEach(leaderAddress -> {
            NetworkCommand apiIterKeys = new ApiIterKeys_NC()
                    .setReceiverAddress(leaderAddress)
                    .setContextId(contextId);
            Set<String> keys = null;
            try {
                if (leaderAddress.equals(getSettings().getAddress())) {
                    // this is the leader so get keys from leader without routing
                    keys = _iterateKeys();
                } else {
                    keys = routeRequestAndGet(apiIterKeys);
                }
                if (keys == null) {
                    Logger.warn(logMsg("Null keys received from leader: " + leaderAddress.toString()));
                }
            } catch (OperationFailedError e) {
                Logger.warn(e, logMsg("Operation failed while retrieving keys from leader: " + leaderAddress.toString()));
            }
            if (keys != null) {
                keySet.addAll(keys);
            }
        });
        return keySet;
    }
    void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Set<String> keys = _iterateKeys();
        sendResponse(
                new LeaderResponse_NC()
                        .setRequest("ApiIterKeys_NC")
                        .setPayload(keys)
                        .ofRequest(iterKeysNc)
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
            Logger.error(e, logMsg(e + ""));
        }

        sendResponse(
                new LeaderElectionResponse_NC()
                        .setBucketIndex(ler.getBucketIndex())
                        .setSuccess(isSuccess)
                        .ofRequest(ler)
                        .setContextId(contextId)
        );
    }

    private boolean requestLeaderElectionForBucket(Address address, int bucketIndex) {

        AtomicBoolean isSuccess = new AtomicBoolean(false);
        Predicate<ICommand> cdHandler = (cmd) -> {
            if (cmd instanceof LeaderElectionResponse_NC) {
                LeaderElectionResponse_NC ler = (LeaderElectionResponse_NC) cmd;
                isSuccess.set(ler.getBucketIndex() == bucketIndex && ler.isSuccess());
                return true;
            }
            return false;
        };
        NetworkCommand ler = new LeaderElectionRequest_NC()
                .setBucketIndex(bucketIndex)
                .setReceiverAddress(address)
                .setContextId(contextId);
        if (sendMsgWithCallback(ler, cdHandler).awaitResponses()) {
            return isSuccess.get();
        }
        return false;
    }

    void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        node.handleCmd(sendFailIc.getNackNC());
//        pinger.registerUnreachableAddress(failedCommand.getReceiverAddress());
    }

    @Override
    public void close() {
    }
}
