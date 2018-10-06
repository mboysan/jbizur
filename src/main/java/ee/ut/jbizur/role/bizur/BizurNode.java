package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.network.messenger.Multicaster;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import ee.ut.jbizur.protocol.internal.NodeDead_IC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.role.RoleValidation;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BizurNode extends Role {
    private boolean isReady;
    protected BucketContainer bucketContainer;
    private CountDownLatch bucketInitLatch;

    BizurNode(BizurSettings settings) throws InterruptedException {
        this(settings, null, null, null, null);
    }

    @ForTestingOnly
    protected BizurNode(BizurSettings settings,
                        Multicaster multicaster,
                        IMessageSender messageSender,
                        IMessageReceiver messageReceiver,
                        CountDownLatch readyLatch) throws InterruptedException {
        super(settings, multicaster, messageSender, messageReceiver, readyLatch);

        this.isReady = false;
        initBuckets();
    }

    protected void initBuckets() {
        this.bucketContainer = createBucketContainer();
        this.bucketInitLatch = new CountDownLatch(BizurConfig.getBucketCount());
    }

    protected BucketContainer createBucketContainer() {
        return new BucketContainer(BizurConfig.getBucketCount()).initBuckets();
    }

    @Override
    public BizurSettings getSettings() {
        return (BizurSettings) super.getSettings();
    }

    protected void checkReady() {
        RoleValidation.checkStateAndError(isReady, "Bizur node is not ready.");
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.<Void>supplyAsync(() -> {
            try {
                long multicastIntervalSec = NodeConfig.getMulticastIntervalMs();
                while (!checkNodesDiscovered()) {
                    Thread.sleep(multicastIntervalSec);
                }
                isReady = initLeaderPerBucketElectionFlow();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
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
                        .setSenderAddress(getSettings().getAddress());
                sendMessage(pleaseVote);
            });

            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    localBucket.updateLeader(true);
                }
            }
        } finally {
            detachMsgListener(listener);
        }
    }

    private void pleaseVote(PleaseVote_NC pleaseVoteNc) {
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
                    .setSenderAddress(getSettings().getAddress());

            bucketInitLatch.countDown();

        } else if(electId == localBucket.getVotedElectId() && source.isSame(localBucket.getLeaderAddress())) {
            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress());
        } else {
            vote = new NackVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress());
        }
        sendMessage(vote);
    }

    private void _pleaseVote(PleaseVote_NC pleaseVoteNc) {
        pleaseVote(pleaseVoteNc);
    }

    protected boolean initLeaderPerBucketElectionFlow() throws InterruptedException {
        for (int i = 0; i < bucketContainer.getNumBuckets(); i++) {
            Bucket localBucket = bucketContainer.getBucket(i);
            electLeaderForBucket(localBucket, localBucket.getIndex(), false);
        }
        return bucketInitLatch.await(BizurConfig.getBucketSetupTimeoutSec(), TimeUnit.SECONDS);
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
        if (localBucket.getLeaderAddress() == null || forceElection) {
            startElection(localBucket.getIndex());
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
                        .setSenderAddress(getSettings().getAddress());
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

    private void replicaWrite(ReplicaWrite_NC replicaWriteNc){
        BucketView recvBucketView = replicaWriteNc.getBucketView();
        Bucket localBucket = bucketContainer.getBucket(recvBucketView.getIndex());

        Address source = replicaWriteNc.getSenderAddress();

        NetworkCommand response;
        if(recvBucketView.getVerElectId() < localBucket.getVotedElectId()){
            response = new NackWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress());
        } else {
            localBucket.replaceBucketForReplicationWith(recvBucketView);
            response = new AckWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress());
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
                        .setSenderId(getSettings().getRoleId());
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

    private void replicaRead(ReplicaRead_NC replicaReadNc){
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();
        Bucket localBucket = bucketContainer.getBucket(index);

        if(electId < localBucket.getVotedElectId()){
            NetworkCommand nackRead = new NackRead_NC()
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(source);
            sendMessage(nackRead);
        } else {
            localBucket.setVotedElectId(electId);
            localBucket.setLeaderAddress(source);

            NetworkCommand ackRead = new AckRead_NC()
                    .setBucketView(localBucket.createView())
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(source);
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
                        .setMsgId(listener.getMsgId());
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

    protected <T> T routeRequestAndGet(NetworkCommand command) {
        return routeRequestAndGet(command, command.getRetryCount());
    }

    protected <T> T routeRequestAndGet(NetworkCommand command, int retryCount) throws OperationFailedError {
        if (retryCount < 0) {
            throw new OperationFailedError(logMsg("Routing failed for command: " + command));
        }
        SyncMessageListener listener = SyncMessageListener.build()
                .withTotalProcessCount(1)
                .withDebugInfo(logMsg("routeRequestAndGet : " + command))
                .registerHandler(Nack_NC.class, (cmd,lst) -> {
                    lst.getPassedObjectRef().set(new SendFail_IC(cmd));
                    lst.end();
                })
                .registerHandler(LeaderResponse_NC.class, (cmd, lst) -> {
                    lst.getPassedObjectRef().set(cmd.getPayload());
                    lst.end();
                });
        attachMsgListener(listener);
        try {
            command.setMsgId(listener.getMsgId());
            Logger.debug(logMsg("routing request to leader retryLeft=[ " + retryCount + "]: " + command));
            sendMessage(command);

            if (listener.waitForResponses()) {
                T rsp = (T) listener.getPassedObjectRef().get();
                if(!(rsp instanceof SendFail_IC)) {
                    return rsp;
                } else {
                    Logger.warn(logMsg("Send failed: " + rsp.toString()));
                }
            }

            return routeRequestAndGet(command, retryCount - 1);

        } finally {
            detachMsgListener(listener);
        }
    }

    public String get(String key) {
        checkReady();
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
                        .setSenderAddress(getSettings().getAddress()));
    }
    private void getByLeader(ApiGet_NC getNc) {
        String val = _get(getNc.getKey());
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiGet_NC-[key=" + getNc.getKey() + "]")
                        .setPayload(val)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(getNc.getMsgId())
        );
    }

    public boolean set(String key, String val) {
        checkReady();
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
                        .setSenderAddress(getSettings().getAddress()));
    }
    private void setByLeader(ApiSet_NC setNc) {
        boolean isSuccess = _set(setNc.getKey(), setNc.getVal());
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiSet_NC-[key=" + setNc.getKey() + ", val=" + setNc.getVal() + "]")
                        .setPayload(isSuccess)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(setNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(setNc.getMsgId())
        );
    }

    public boolean delete(String key) {
        checkReady();
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
                        .setSenderAddress(getSettings().getAddress()));
    }
    private void deleteByLeader(ApiDelete_NC deleteNc) {
        boolean isDeleted = _delete(deleteNc.getKey());
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiDelete_NC-[key=" + deleteNc.getKey() + "]")
                        .setPayload(isDeleted)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(deleteNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(deleteNc.getMsgId())
        );
    }

    public Set<String> iterateKeys() {
        checkReady();
        Set<String> keySet = new HashSet<>();
        Set<Address> bucketLeaders = bucketContainer.collectAddressesWithBucketLeaders();
        bucketLeaders.forEach(leaderAddress -> {
            NetworkCommand apiIterKeys = new ApiIterKeys_NC()
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(leaderAddress)
                    .setSenderAddress(getSettings().getAddress());
            Set<String> keys = routeRequestAndGet(apiIterKeys);
            if (keys == null) {
                Logger.warn(logMsg("Null keys received from leader: " + leaderAddress.toString()));
            } else {
                keySet.addAll(keys);
            }
        });
        return keySet;
    }
    private void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Set<String> keys = _iterateKeys();
        sendMessage(
                new LeaderResponse_NC()
                        .setRequest("ApiIterKeys_NC")
                        .setPayload(keys)
                        .setSenderId(getSettings().getRoleId())
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(iterKeysNc.getSenderAddress())
                        .setMsgId(iterKeysNc.getMsgId())
        );
    }

    /* ***************************************************************************
     * Failure Handling
     * ***************************************************************************/

    private void handleSendFailureWithoutRetry(SendFail_IC sendFailIc) {
        NetworkCommand failedCommand = sendFailIc.getNetworkCommand();
        handleNetworkCommand(new Nack_NC()
                .setSenderId(failedCommand.getSenderId())
                .setSenderAddress(failedCommand.getSenderAddress())
                .setReceiverAddress(failedCommand.getReceiverAddress())
                .setMsgId(failedCommand.getMsgId())
        );
//        pinger.registerUnreachableAddress(failedCommand.getReceiverAddress());
    }

    /* ***************************************************************************
     * Message Handling
     * ***************************************************************************/

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        super.handleNetworkCommand(command);

        if (command instanceof ReplicaWrite_NC){
            replicaWrite((ReplicaWrite_NC) command);
        }
        if (command instanceof ReplicaRead_NC){
            replicaRead(((ReplicaRead_NC) command));
        }
        if (command instanceof PleaseVote_NC) {
            _pleaseVote(((PleaseVote_NC) command));
        }

        /* Internal API routed requests */
        if(command instanceof ApiGet_NC){
            getByLeader((ApiGet_NC) command);
        }
        if(command instanceof ApiSet_NC){
            setByLeader((ApiSet_NC) command);
        }
        if(command instanceof ApiDelete_NC){
            deleteByLeader((ApiDelete_NC) command);
        }
        if(command instanceof ApiIterKeys_NC){
            iterateKeysByLeader((ApiIterKeys_NC) command);
        }

        String uniqueKey = UUID.randomUUID().toString();
        Object payload = uniqueKey;
        /* Internal API routed requests */
        if(command instanceof ClientApiGet_NC){
            payload = get(((ClientApiGet_NC) command).getKey());
        }
        if(command instanceof ClientApiSet_NC){
            payload = set(((ClientApiSet_NC) command).getKey(), ((ClientApiSet_NC) command).getVal());
        }
        if(command instanceof ClientApiDelete_NC){
            payload = delete(((ClientApiDelete_NC) command).getKey());
        }
        if(command instanceof ClientApiIterKeys_NC){
            payload = iterateKeys();
        }

        if(!uniqueKey.equals(payload)) {
            sendMessage(new LeaderResponse_NC()
                    .setPayload(payload)
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(command.getSenderAddress())
                    .setSenderAddress(getSettings().getAddress())
                    .setMsgId(command.getMsgId()));
        }
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {
        if(command instanceof SendFail_IC) {
            handleSendFailureWithoutRetry((SendFail_IC) command);
        }
        if(command instanceof NodeDead_IC) {
            handleNodeFailure(((NodeDead_IC) command).getNodeAddress());
        }
    }

    private class SimpleLock {
        private boolean isLocked = false;

        public synchronized void lock() throws InterruptedException{
            while(isLocked){
                wait();
            }
            isLocked = true;
        }

        public synchronized void unlock(){
            isLocked = false;
            notify();
        }
    }
}
