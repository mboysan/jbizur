package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
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
import ee.ut.jbizur.role.RoleSettings;
import ee.ut.jbizur.role.RoleValidation;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BizurNode extends Role {
    private boolean isReady;
    private Bucket[] localBuckets;
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
        initNode();
        initBuckets();
    }

    @Override
    public BizurSettings getSettings() {
        return (BizurSettings) super.getSettings();
    }

    protected void initNode() {
    }

    protected void initBuckets() {
        int count = getSettings().getNumBuckets();
        localBuckets = new Bucket[count];
        for (int i = 0; i < count; i++) {
            Bucket b = new Bucket()
                    .setIndex(i);
            localBuckets[i] = b;
        }
        bucketInitLatch = new CountDownLatch(count);
        Logger.info("buckets are initialized!");
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
        String msgId = RoleSettings.generateMsgId();
        SyncMessageListener listener = new SyncMessageListener(msgId, getSettings().getProcessCount()) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if (command instanceof AckVote_NC) {
                    incrementAckCount();
                    getProcessesLatch().countDown();
                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof Nack_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);
        try{
            Bucket localBucket = getLocalBucket(bucketIndex);
            int electId = localBucket.getVer().incrementAndGetElectId();
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand pleaseVote = new PleaseVote_NC()
                        .setBucketIndex(bucketIndex)
                        .setElectId(electId)
                        .setMsgId(msgId)
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
        Bucket localBucket = getLocalBucket(bucketIndex);

        NetworkCommand vote;
        if (electId > localBucket.getVer().getVotedElectId()) {
            localBucket.getVer().setVotedElectedId(electId);
            localBucket.setLeaderAddress(source);
            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress());

            bucketInitLatch.countDown();

        } else if(electId == localBucket.getVer().getVotedElectId() && source.isSame(localBucket.getLeaderAddress())) {
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
        int bucketIndex = pleaseVoteNc.getBucketIndex();
        lockBucket(bucketIndex);
        try {
            pleaseVote(pleaseVoteNc);
        } finally {
            unlockBucket(bucketIndex);
        }
    }

    private boolean initLeaderPerBucketElectionFlow() throws InterruptedException {
        return initLeaderPerBucketElectionFlow(IdUtils.getAddressQueue(getSettings().getMemberAddresses()));
    }

    private boolean initLeaderPerBucketElectionFlow(Queue<Address> addressQueue) throws InterruptedException {
        Address nextAddr = addressQueue.poll();
        if (nextAddr == null) {
            throw new IllegalStateException(logMsg("leader per bucket election flow could not be initialized"));
        }
        if (nextAddr.isSame(getSettings().getAddress())) {
            for (Bucket localBucket : localBuckets) {
                Logger.info(logMsg("initializing election process on bucket idx=" + localBucket.getIndex()));
                resolveLeader(localBucket);
            }
        } else {
            if (pingAddress(nextAddr)) {
                // the other address will take care of the election processes, no worries.
                Logger.info(logMsg("election process will be handled by: " + nextAddr));
            } else {
                Logger.warn(logMsg("address '" + nextAddr + "' unreachable, reinit election process..."));
                initLeaderPerBucketElectionFlow(addressQueue);
            }
        }
        if (bucketInitLatch.await(BizurConfig.getBucketSetupTimeoutSec(), TimeUnit.SECONDS)) {
            return true;
        }
        Logger.error("timeout bucket setup.");
        return false;
    }

    protected Address resolveLeader(Bucket localBucket) {
        if (localBucket.getLeaderAddress() == null) {
            startElection(localBucket.getIndex());
        }
        return localBucket.getLeaderAddress();
    }

    /* ***************************************************************************
     * Algorithm 2 - Bucket Replication: Write
     * ***************************************************************************/

    private boolean write(Bucket bucketToWrite) {
        Bucket localBucket = getLocalBucket(bucketToWrite.getIndex());
        bucketToWrite.getVer().setElectId(localBucket.getVer().getElectId());
        bucketToWrite.getVer().incrementCounter();

        String msgId = RoleSettings.generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId, getSettings().getProcessCount()) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if(command instanceof AckWrite_NC){
                    incrementAckCount();
                    getProcessesLatch().countDown();
                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof Nack_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);
        try {
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaWrite = new ReplicaWrite_NC()
                        .setBucketView(bucketToWrite.createView())
                        .setMsgId(msgId)
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
        Bucket bucketToReplicate = Bucket.createBucketFromView(replicaWriteNc.getBucketView());
        Address source = replicaWriteNc.getSenderAddress();
        Bucket localBucket = getLocalBucket(bucketToReplicate.getIndex());

        NetworkCommand response;
        if(bucketToReplicate.getVer().getElectId() < localBucket.getVer().getVotedElectId()){
            response = new NackWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getSettings().getAddress());
        } else {
            localBucket.replaceWithBucket(bucketToReplicate);
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
        Bucket localBucket = getLocalBucket(index);
        int electId = localBucket.getVer().getElectId();
        if(!ensureRecovery(electId, index)){
            return null;
        }

        String msgId = RoleSettings.generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId, getSettings().getProcessCount()) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if(command instanceof AckRead_NC){
                    incrementAckCount();
                    getProcessesLatch().countDown();
                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof Nack_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);
        try {
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaRead = new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(receiverAddress)
                        .setMsgId(msgId)
                        .setSenderId(getSettings().getRoleId());
                sendMessage(replicaRead);
            });

            Bucket toReturn = null;
            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    toReturn = getLocalBucket(index);
                } else {
                    localBucket.updateLeader(false);
                }
            }
            return toReturn;
        } finally {
            detachMsgListener(listener);
        }
    }

    private void replicaRead(ReplicaRead_NC replicaReadNc){
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();
        Bucket localBucket = getLocalBucket(index);

        if(electId < localBucket.getVer().getVotedElectId()){
            NetworkCommand nackRead = new NackRead_NC()
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(source);
            sendMessage(nackRead);
        } else {
            localBucket.getVer().setVotedElectedId(electId);
            localBucket.setLeaderAddress(source);

            NetworkCommand ackRead = new AckRead_NC()
                    .setBucketView(getLocalBucket(index).createView())
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
        Bucket localBucket = getLocalBucket(index);
        if(electId == localBucket.getVer().getElectId()) {
            return true;
        }

        String msgId = RoleSettings.generateMsgId();

        final Bucket[] maxVerBucket = {null};
        SyncMessageListener listener = new SyncMessageListener(msgId, getSettings().getProcessCount()) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if(command instanceof AckRead_NC){
                    AckRead_NC ackRead = ((AckRead_NC) command);
                    Bucket bucket = Bucket.createBucketFromView(ackRead.getBucketView());

                    if(maxVerBucket[0] == null){
                        maxVerBucket[0] = bucket;
                    } else {
                        if(bucket.getVer().compareTo(maxVerBucket[0].getVer()) > 0){
                            maxVerBucket[0] = bucket;
                        }
                    }

                    incrementAckCount();
                    getProcessesLatch().countDown();

                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof Nack_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);
        try {
            getSettings().getMemberAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaRead = new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setSenderId(getSettings().getRoleId())
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(receiverAddress)
                        .setMsgId(msgId);
                sendMessage(replicaRead);
            });

            boolean isSuccess = false;
            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    maxVerBucket[0].getVer().setElectId(electId);
                    maxVerBucket[0].getVer().setCounter(0);
                    isSuccess = write(maxVerBucket[0]);
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
        int index = hashKey(key);
        lockBucket(index);
        try {
            Bucket bucket = read(index);
            if(bucket != null){
                return bucket.getOp(key);
            }
            return null;
        } finally {
            unlockBucket(index);
        }
    }

    private boolean _set(String key, String value){
        int index = hashKey(key);
        lockBucket(index);
        try {
            Bucket bucket = read(index);
            if (bucket != null) {
                bucket.putOp(key, value);
                return write(bucket);
            }
            return false;
        } finally {
            unlockBucket(index);
        }
    }

    private boolean _delete(String key) {
        int index = hashKey(key);
        lockBucket(index);
        try {
            Bucket bucket = read(index);
            if(bucket != null){
                bucket.removeOp(key);
                return write(bucket);
            }
            return false;
        } finally {
            unlockBucket(index);
        }
    }

    private Set<String> _iterateKeys() {
        Set<String> res = new HashSet<>();
        for (int index = 0; index < getSettings().getNumBuckets(); index++) {
            lockBucket(index);
            try {
                Bucket bucket = read(index);
                if(bucket != null){
                    res.addAll(bucket.getKeySet());
                }
            } finally {
                unlockBucket(index);
            }
        }
        return res;
    }

    /**
     * Taken from <a href="https://algs4.cs.princeton.edu/34hash/">34hash site</a>.
     * @param s key to hash.
     * @return index of the bucket.
     */
    protected int hashKey(String s) {
        int R = 31;
        int hash = 0;
        for (int i = 0; i < s.length(); i++)
            hash = (R * hash + s.charAt(i)) % getSettings().getNumBuckets();
        return hash;
    }

    private void lockBucket(int index) {
        getLocalBucket(index).lock();
    }

    private void unlockBucket(int index) {
        getLocalBucket(index).unlock();
    }

    protected Bucket getLocalBucket(int index) {
        return localBuckets[index];
    }

    protected Bucket getLocalBucket(String key) {
        return localBuckets[hashKey(key)];
    }


    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    protected  <T> T routeRequestAndGet(NetworkCommand command) {
        return routeRequestAndGet(command, 1);
    }

    protected <T> T routeRequestAndGet(NetworkCommand command, int retryCount) throws OperationFailedError {
        final Object[] resp = new Object[1];
        String msgId = RoleSettings.generateMsgId();
        SyncMessageListener listener = new SyncMessageListener(msgId, 1) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if(command instanceof Nack_NC) {
                    resp[0] = new SendFail_IC(command);
                    end();
                } else if (command instanceof LeaderResponse_NC){
                    resp[0] = command.getPayload();
                    end();
                }
            }
        };
        attachMsgListener(listener);
        try {
            command.setMsgId(msgId);
            sendMessage(command);

            if (listener.waitForResponses()) {
                T rsp = (T) resp[0];
                if(!(rsp instanceof SendFail_IC)) {
                    return rsp;
                } else {
                    Logger.warn("Send failed: " + rsp.toString());
                }
            }

            throw new OperationFailedError("Routing failed for command: " + command);

        } finally {
            detachMsgListener(listener);
        }
    }

    public String get(String key) {
        checkReady();
        Bucket bucket = getLocalBucket(key);
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
                        .setPayload(val)
                        .setReceiverAddress(getNc.getSenderAddress())
                        .setMsgId(getNc.getMsgId())
        );
    }

    public boolean set(String key, String val) {
        checkReady();
        Bucket bucket = getLocalBucket(key);
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
                        .setPayload(isSuccess)
                        .setReceiverAddress(setNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setMsgId(setNc.getMsgId())
        );
    }

    public boolean delete(String key) {
        checkReady();
        Bucket bucket = getLocalBucket(key);
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
                        .setPayload(isDeleted)
                        .setReceiverAddress(deleteNc.getSenderAddress())
                        .setSenderAddress(getSettings().getAddress())
                        .setSenderId(getSettings().getRoleId())
                        .setMsgId(deleteNc.getMsgId())
        );
    }

    public Set<String> iterateKeys() {
        checkReady();
        /*
        if(isLeader()){
            return _iterateKeys();
        }
        return routeRequestAndGet(
                new ApiIterKeys_NC()
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(leaderAddress.get())
                        .setSenderAddress(getSettings().getAddress()));*/
        throw new UnsupportedOperationException("iterate keys not supported yet");
    }
    private void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        /*
        Set<String> keys = _iterateKeys();
        sendMessage(
                new LeaderResponse_NC()
                        .setPayload(keys)
                        .setReceiverAddress(iterKeysNc.getSenderAddress())
                        .setMsgId(iterKeysNc.getMsgId())
        );*/
        throw new UnsupportedOperationException("iterate keys not supported yet");
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
