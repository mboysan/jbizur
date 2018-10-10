package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BizurRun {

    protected BizurNode node;

    private final BucketContainer bucketContainer;
    private final int contextId;

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

    private void attachMsgListener(SyncMessageListener listener) {
        node.attachMsgListener(listener);
    }
    private void detachMsgListener(SyncMessageListener listener) {
        node.detachMsgListener(listener);
    }
    private String logMsg(String msg) {
        return node.logMsg(msg);
    }
    private BizurSettings getSettings() {
        return node.getSettings();
    }
    private void sendMessage(NetworkCommand command) {
        if (command.getReceiverAddress().isSame(getSettings().getAddress())) {
            Logger.debug("OUT " + logMsg(command.toString()));
            node.handleNetworkCommand(command);
        } else {
            node.sendMessage(command);
        }
    }
    private boolean pingAddress(Address address) {
        return node.pingAddress(address);
    }
    private <T> T routeRequestAndGet(NetworkCommand command) {
        return node.routeRequestAndGet(command);
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    protected void startElection(int bucketIndex) {
        SyncMessageListener listener = SyncMessageListener.buildWithDefaultHandlers()
                .withTotalProcessCount(getSettings().getProcessCount())
                .withDebugInfo(logMsg("startElection"));
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
            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress())
                    .setContextId(pleaseVoteNc.getContextId());
        } else if(electId == localBucket.getVotedElectId() && source.isSame(localBucket.getLeaderAddress())) {
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
            int maxRetry = BizurConfig.getBucketLeaderElectionRetryCount();
            while (retry < maxRetry) {
                electLeaderForBucket(localBucket, localBucket.getIndex(), true);
                if (localBucket.getLeaderAddress() != null) {
                    break;
                }
                Thread.sleep(100);
                retry++;
            }
            if (retry >= maxRetry) {
                return false;
            }
        }
        return true;
    }

    protected void electLeaderForBucket(Bucket localBucket, int startIdx, boolean forceElection) {
        Address nextAddr = IdUtils.nextAddressInUnorderedSet(getSettings().getMemberAddresses(), startIdx);
        if (nextAddr.isSame(getSettings().getAddress())) {
            Logger.info(logMsg("initializing election process on bucket idx=" + localBucket.getIndex()));
            resolveLeader(localBucket, forceElection);
        } else {
            if (pingAddress(nextAddr)) {
                Logger.info(logMsg("election process will be handled by: " + nextAddr + " for bucket idx=" + localBucket.getIndex()));
            } else {
                Logger.warn(logMsg("address '" + nextAddr + "' unreachable, reinit election process for bucket idx=" + localBucket.getIndex() + " ..."));
                electLeaderForBucket(localBucket, startIdx + 1, forceElection);
            }
        }
    }

    protected Address resolveLeader(Bucket localBucket) {
        return resolveLeader(localBucket, false);
    }

    protected Address resolveLeader(Bucket localBucket, boolean forceElection) {
        int bucketIndex = localBucket.getIndex();
        if (forceElection) {
            bucketContainer.lockBucket(bucketIndex);
            try {
                startElection(bucketIndex);
            } finally {
                bucketContainer.unlockBucket(bucketIndex);
            }
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
                .withTotalProcessCount(getSettings().getProcessCount())
                .withDebugInfo(logMsg("write"));
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
                .withTotalProcessCount(getSettings().getProcessCount())
                .withDebugInfo(logMsg("read"));
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
        if (bucket.isLeader()) {
            return _get(key);
        }
        Address lead = resolveLeader(bucket);
        return routeRequestAndGet(
                new ApiGet_NC()
                        .setKey(key)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(lead)
                        .setSenderAddress(getSettings().getAddress())
                        .setContextId(contextId));
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
        if(bucket.isLeader()){
            return _set(key, val);
        }
        Address lead = resolveLeader(bucket);
        return routeRequestAndGet(
                new ApiSet_NC()
                        .setKey(key)
                        .setVal(val)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(lead)
                        .setSenderAddress(getSettings().getAddress())
                        .setContextId(contextId)
        );
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
        if(bucket.isLeader()){
            return _delete(key);
        }
        Address lead = resolveLeader(bucket);
        return routeRequestAndGet(
                new ApiDelete_NC()
                        .setKey(key)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(lead)
                        .setSenderAddress(getSettings().getAddress())
                        .setContextId(contextId)
        );
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
            Set<String> keys = routeRequestAndGet(apiIterKeys);
            if (keys == null) {
                Logger.warn(logMsg("Null keys received from leader: " + leaderAddress.toString()));
            } else {
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

    void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        NetworkCommand failedCommand = sendFailIc.getNetworkCommand();
        node.handleNetworkCommand(new Nack_NC()
                .setSenderId(failedCommand.getSenderId())
                .setSenderAddress(failedCommand.getSenderAddress())
                .setReceiverAddress(failedCommand.getReceiverAddress())
                .setMsgId(failedCommand.getMsgId())
        );
//        pinger.registerUnreachableAddress(failedCommand.getReceiverAddress());
    }
}
