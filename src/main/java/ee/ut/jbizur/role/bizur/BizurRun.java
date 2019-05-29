package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.config.LogConf;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.exceptions.LeaderResolutionFailedError;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.io.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class BizurRun {

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

    protected void attachMsgListener(SyncMessageListener listener) {
        node.attachMsgListener(listener);
    }
    protected void detachMsgListener(SyncMessageListener listener) {
        node.detachMsgListener(listener);
    }
    protected String logMsg(String msg) {
        return node.logMsg(msg);
    }
    protected BizurSettings getSettings() {
        return node.getSettings();
    }
    protected void sendMessage(NetworkCommand command) {
        if (command.getReceiverAddress().equals(getSettings().getAddress())) {
            if (LogConf.isDebugEnabled()) {
                Logger.debug("OUT " + logMsg(command.toString()));
            }
            node.handleNetworkCommand(command);
        } else {
            node.sendMessage(command);
        }
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
        SyncMessageListener listener = SyncMessageListener.buildWithDefaultHandlers()
                .withTotalProcessCount(getSettings().getProcessCount());
        if (LogConf.isDebugEnabled()) {
            listener.withDebugInfo(logMsg("startElection"));
        }
        attachMsgListener(listener);
        try{
            Bucket localBucket = bucketContainer.getBucket(bucketIndex);
            int electId = localBucket.incrementAndGetElectId();
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand pleaseVote = new PleaseVote_NC()
                        .setBucketIndex(bucketIndex)
                        .setElectId(electId)
                        .setMsgId(listener.getMsgId())
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(receiverAddress)
                        .setSenderAddress(getSettings().getAddress())
                        .setContextId(contextId);
                sendMessage(pleaseVote);
            });

            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    /* following is done to guarantee that in case the leader discards the PleaseVote request,
                       the bucket leader is set properly for the first time. */
                    localBucket.setVotedElectId(electId);
                    localBucket.setLeaderAddress(getSettings().getAddress());

                    localBucket.updateLeader(true);
                }
            }
        } finally {
            detachMsgListener(listener);
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

            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(pleaseVoteNc.getContextId());
        } else if(electId == localBucket.getVotedElectId() && source.equals(localBucket.getLeaderAddress())) {
            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(pleaseVoteNc.getContextId());
        } else {
            vote = new NackVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(pleaseVoteNc.getContextId());
        }
        sendMessage(vote);
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

        SyncMessageListener listener = SyncMessageListener.buildWithDefaultHandlers()
                .withTotalProcessCount(getSettings().getProcessCount());
        if (LogConf.isDebugEnabled()) {
            listener.withDebugInfo(logMsg("write"));
        }
        attachMsgListener(listener);
        try {
            BucketView bucketViewToSend = bucketToWrite.createView();
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaWrite = new ReplicaWrite_NC()
                        .setBucketView(bucketViewToSend)
                        .setMsgId(listener.getMsgId())
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(receiverAddress)
                        .setSenderAddress(getSettings().getAddress())
                        .setContextId(contextId);
                sendMessage(replicaWrite);
            });

            boolean isSuccess = false;
            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    isSuccess = true;
                } else {
                    localBucket.updateLeader(false);
                }
            }
            return isSuccess;
        } finally {
            detachMsgListener(listener);
        }
    }

    void replicaWrite(ReplicaWrite_NC replicaWriteNc) {
        BucketView recvBucketView = replicaWriteNc.getBucketView();
        Bucket localBucket = bucketContainer.getBucket(recvBucketView.getIndex());

        Address source = replicaWriteNc.getSenderAddress();

        NetworkCommand response;
        if(recvBucketView.getVerElectId() < localBucket.getVotedElectId()){
            response = new NackWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(replicaWriteNc.getContextId());
        } else {
            localBucket.replaceBucketForReplicationWith(recvBucketView);
            response = new AckWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(replicaWriteNc.getContextId());
        }
        sendMessage(response);
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

        SyncMessageListener listener = SyncMessageListener.buildWithDefaultHandlers()
                .withTotalProcessCount(getSettings().getProcessCount());
        if (LogConf.isDebugEnabled()) {
            listener.withDebugInfo(logMsg("read"));
        }
        attachMsgListener(listener);
        try {
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaRead = new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(receiverAddress)
                        .setMsgId(listener.getMsgId())
                        .setSenderId(getSettings().getRoleId())
                        .setContextId(contextId);
                sendMessage(replicaRead);
            });

            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    return localBucket;
                } else {
                    localBucket.updateLeader(false);
                }
            }
            return null;
        } finally {
            detachMsgListener(listener);
        }
    }

    void replicaRead(ReplicaRead_NC replicaReadNc){
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getBucket(index);

        if(electId < localBucket.getVotedElectId()){
            NetworkCommand nackRead = new NackRead_NC()
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(source)
                    .setContextId(replicaReadNc.getContextId());
            sendMessage(nackRead);
        } else {
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);
            localBucket.updateLeader(source.equals(getSettings().getAddress()));

            NetworkCommand ackRead = new AckRead_NC()
                    .setBucketView(localBucket.createView())
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(source)
                    .setContextId(replicaReadNc.getContextId());
            sendMessage(ackRead);
        }
    }

    /* ***************************************************************************
     * Algorithm 4 - Bucket Replication: Recovery
     * ***************************************************************************/

    private boolean ensureRecovery(int electId, int index) {
        Bucket localBucket = bucketContainer.getBucket(index);
        if(electId == localBucket.getVerElectId()) {
            return true;
        }

        SyncMessageListener listener = SyncMessageListener.build()
                .withTotalProcessCount(getSettings().getProcessCount())
                .registerHandler(AckRead_NC.class, (cmd, lst) -> {
                    AckRead_NC ackRead = ((AckRead_NC) cmd);
                    BucketView bucketView = ackRead.getBucketView();
                    AtomicReference<Object> maxVerBucketView = lst.getPassedObjectRef();
                    synchronized (maxVerBucketView) {
                        if (!maxVerBucketView.compareAndSet(null, bucketView)) {
                            if (bucketView.compareVersion((BucketView) maxVerBucketView.get()) > 0) {
                                maxVerBucketView.set(bucketView);
                            }
                        }
                    }
                });

        attachMsgListener(listener);
        try {
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaRead = new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setSenderId(getSettings().getRoleId())
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(receiverAddress)
                        .setMsgId(listener.getMsgId())
                        .setContextId(contextId);
                sendMessage(replicaRead);
            });

            boolean isSuccess = false;
            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    Bucket maxVerBucket = ((BucketView) listener.getPassedObjectRef().get()).createBucket(bucketContainer);
                    maxVerBucket.setVerElectId(electId);
                    maxVerBucket.setVerCounter(0);
                    isSuccess = write(maxVerBucket);
                } else {
                    localBucket.updateLeader(false);
                }
            }
            return isSuccess;
        } finally {
            detachMsgListener(listener);
        }
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
        bucketContainer.bucketIndices(getSettings().getAddress()).forEach(bucketIdx -> {
            bucketContainer.lockBucket(bucketIdx);
            try {
                Bucket bucket = read(bucketIdx);
                if(bucket != null){
                    res.addAll(bucket.getKeySet());
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
                            .setSenderId(getSettings().getRoleId())
                            .setReceiverAddress(lead)
                            .setSenderAddress(getSettings().getAddress())
                            .setContextId(contextId));
        } catch (OperationFailedError e) {
            Logger.warn(e);
            electLeaderForBucket(bucket, bucket.getIndex(), true);
            return get(key);
        }
    }
    void getByLeader(ApiGet_NC getNc) {
        String val = _get(getNc.getKey());
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiGet_NC-[key=" + getNc.getKey() + "]")
                        .setPayload(val)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(getNc.getMsgId())
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
                            .setSenderId(getSettings().getRoleId())
                            .setReceiverAddress(lead)
                            .setSenderAddress(getSettings().getAddress())
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
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiSet_NC-[key=" + setNc.getKey() + ", val=" + setNc.getVal() + "]")
                        .setPayload(isSuccess)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(setNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(setNc.getMsgId())
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
                            .setSenderId(getSettings().getRoleId())
                            .setReceiverAddress(lead)
                            .setSenderAddress(getSettings().getAddress())
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
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiDelete_NC-[key=" + deleteNc.getKey() + "]")
                        .setPayload(isDeleted)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(deleteNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(deleteNc.getMsgId())
                        .setContextId(contextId)
        );
    }

    Set<String> iterateKeys() {
        Set<String> keySet = new HashSet<>();
        Set<Address> bucketLeaders = bucketContainer.collectAddressesWithBucketLeaders();
        bucketLeaders.forEach(leaderAddress -> {
            NetworkCommand apiIterKeys = new ApiIterKeys_NC()
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(leaderAddress)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(contextId);
            Set<String> keys = null;
            try {
                keys = routeRequestAndGet(apiIterKeys);
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
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiIterKeys_NC")
                        .setPayload(keys)
                        .setSenderId(getSettings().getRoleId())
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(iterKeysNc.getSenderAddress())
                        .setMsgId(iterKeysNc.getMsgId())
                        .setContextId(contextId)
        );
    }

    /* ***************************************************************************
     * Failure Handling
     * ***************************************************************************/

    void handleLeaderElection(LeaderElectionRequest_NC ler) {
        Bucket localBucket = bucketContainer.getBucket(ler.getBucketIndex());

        NetworkCommand response = new LeaderElectionResponse_NC()
                .setBucketIndex(ler.getBucketIndex())
                .setContextId(contextId)
                .setMsgId(ler.getMsgId())
                .setSenderId(getSettings().getRoleId())
                .setSenderAddress(getSettings().getAddress())
                .setReceiverAddress(ler.getSenderAddress());

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

        ((LeaderElectionResponse_NC) response).setSuccess(isSuccess);
        sendMessage(response);
    }

    private boolean requestLeaderElectionForBucket(Address address, int bucketIndex) {
        SyncMessageListener listener = SyncMessageListener.build()
                .withTotalProcessCount(1)
                .registerHandler(LeaderElectionResponse_NC.class, (cmd, lst) -> {
                    LeaderElectionResponse_NC ler = (LeaderElectionResponse_NC) cmd;
                    lst.getPassedObjectRef().set(ler.getBucketIndex() == bucketIndex && ler.isSuccess());
                    lst.end();
                });
        attachMsgListener(listener);
        try {
            NetworkCommand ler = new LeaderElectionRequest_NC()
                    .setBucketIndex(bucketIndex)
                    .setMsgId(listener.getMsgId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(address)
                    .setSenderId(getSettings().getRoleId());
            if (LogConf.isDebugEnabled()) {
                listener.withDebugInfo(logMsg("leader election request: " + ler));
            }
            sendMessage(ler);
            if (listener.waitForResponses()) {
                return (boolean) listener.getPassedObjectRef().get();
            }
        } finally {
            detachMsgListener(listener);
        }
        return false;
    }

    void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        NetworkCommand failedCommand = sendFailIc.getNetworkCommand();
        node.handleNetworkCommand(new Nack_NC()
                .setSenderId(failedCommand.getSenderId())
                .setSenderAddress(failedCommand.getSenderAddress())
                .setReceiverAddress(failedCommand.getReceiverAddress())
                .setMsgId(failedCommand.getMsgId())
                .setContextId(failedCommand.getContextId())
        );
//        pinger.registerUnreachableAddress(failedCommand.getReceiverAddress());
    }
}
