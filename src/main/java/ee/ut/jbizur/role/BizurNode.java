package ee.ut.jbizur.role;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import org.pmw.tinylog.Logger;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import ee.ut.jbizur.protocol.internal.NodeDead_IC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BizurNode extends Role {

    private static final int BUCKET_COUNT = 1;

    private AtomicInteger electId;
    private AtomicInteger votedElectId;
    private AtomicReference<Address> leaderAddress;
    private CountDownLatch leaderUpdateLatch = new CountDownLatch(1);

    private Bucket[] localBuckets;

    public BizurNode(Address baseAddress) throws InterruptedException {
        this(baseAddress, null, null, null);
    }

    @ForTestingOnly
    protected BizurNode(Address baseAddress,
                        IMessageSender messageSender,
                        IMessageReceiver messageReceiver,
                        CountDownLatch readyLatch) throws InterruptedException {
        super(baseAddress, messageSender, messageReceiver, readyLatch);

        initNode();
        initBuckets(BUCKET_COUNT);
    }

    protected void initNode() {
//        int eId = Math.abs((int) System.currentTimeMillis() + getRoleId().hashCode());
        int eId = 0;
        electId = new AtomicInteger(eId);
        votedElectId = new AtomicInteger(0);
        leaderAddress = new AtomicReference<>(null);
    }

    protected void initBuckets(int count) {
        localBuckets = new Bucket[count];
        for (int i = 0; i < count; i++) {
            Bucket b = new Bucket()
                    .setIndex(i);
            localBuckets[i] = b;
        }
        Logger.info("buckets are initialized!");
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    protected void startElection() {
        electId.set(votedElectId.get());
        electId.incrementAndGet();

        String msgId = GlobalConfig.getInstance().generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId) {
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
            GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
                NetworkCommand pleaseVote = new PleaseVote_NC()
                        .setElectId(electId.get())
                        .setMsgId(msgId)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(receiverAddress)
                        .setSenderAddress(getAddress());
                sendMessage(pleaseVote);
            });

            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    updateLeader(true);
                }
            }
        } finally {
            detachMsgListener(listener);
        }
    }

    private void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        int electId = pleaseVoteNc.getElectId();
        Address source = pleaseVoteNc.getSenderAddress();

        NetworkCommand vote;
        if (electId > votedElectId.get()) {
            votedElectId.set(electId);
            updateLeader(source);
            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        } else if(electId == votedElectId.get() && source.isSame(leaderAddress.get())) {
            vote = new AckVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        } else {
            vote = new NackVote_NC()
                    .setMsgId(pleaseVoteNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        }
        sendMessage(vote);
    }

    /* ***************************************************************************
     * Algorithm 2 - Bucket Replication: Write
     * ***************************************************************************/

    private boolean write(Bucket bucket) {
        bucket.getVer().setElectId(electId.get());
        bucket.getVer().incrementCounter();

        String msgId = GlobalConfig.getInstance().generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId) {
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
            GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaWrite = new ReplicaWrite_NC()
                        .setBucketView(bucket.createView())
                        .setMsgId(msgId)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(receiverAddress)
                        .setSenderAddress(getAddress());
                sendMessage(replicaWrite);
            });

            boolean isSuccess = false;
            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    isSuccess = true;
                } else {
                    updateLeader(false);
                }
            }
            return isSuccess;
        } finally {
            detachMsgListener(listener);
        }
    }

    private void replicaWrite(ReplicaWrite_NC replicaWriteNc){
        Bucket bucket = Bucket.createBucketFromView(replicaWriteNc.getBucketView());
        Address source = replicaWriteNc.getSenderAddress();

        NetworkCommand response;
        if(bucket.getVer().getElectId() < votedElectId.get()){
            response = new NackWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        } else {
            votedElectId.set(bucket.getVer().getElectId());
            updateLeader(source);
            localBuckets[bucket.getIndex()].replaceWithBucket(bucket);

            response = new AckWrite_NC()
                    .setMsgId(replicaWriteNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        }
        sendMessage(response);
    }



    /* ***************************************************************************
     * Algorithm 3 - Bucket Replication: Read
     * ***************************************************************************/

    private Bucket read(int index) {
        if(!ensureRecovery(electId.get(), index)){
            return null;
        }

        String msgId = GlobalConfig.getInstance().generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId) {
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
            GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaRead = new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId.get())
                        .setSenderAddress(getAddress())
                        .setReceiverAddress(receiverAddress)
                        .setMsgId(msgId);
                sendMessage(replicaRead);
            });

            Bucket toReturn = null;
            if(listener.waitForResponses()){
                if(listener.isMajorityAcked()){
                    toReturn = localBuckets[index];
                } else {
                    updateLeader(false);
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

        if(electId < votedElectId.get()){
            NetworkCommand nackRead = new NackRead_NC()
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setSenderAddress(getAddress())
                    .setReceiverAddress(source);
            sendMessage(nackRead);
        } else {
            votedElectId.set(electId);
            updateLeader(source);

            NetworkCommand ackRead = new AckRead_NC()
                    .setBucketView(localBuckets[index].createView())
                    .setMsgId(replicaReadNc.getMsgId())
                    .setSenderId(getRoleId())
                    .setSenderAddress(getAddress())
                    .setReceiverAddress(source);
            sendMessage(ackRead);
        }
    }

    /* ***************************************************************************
     * Algorithm 4 - Bucket Replication: Recovery
     * ***************************************************************************/

    private boolean ensureRecovery(int electId, int index) {
        if(electId == localBuckets[index].getVer().getElectId()) {
            return true;
        }

        String msgId = GlobalConfig.getInstance().generateMsgId();

        final Bucket[] maxVerBucket = {null};
        SyncMessageListener listener = new SyncMessageListener(msgId) {
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
            GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
                NetworkCommand replicaRead = new ReplicaRead_NC()
                        .setIndex(index)
                        .setElectId(electId)
                        .setSenderId(getRoleId())
                        .setSenderAddress(getAddress())
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
                    updateLeader(false);
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
        for (int index = 0; index < BUCKET_COUNT; index++) {
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
    protected static int hashKey(String s) {
        int R = 31;
        int hash = 0;
        for (int i = 0; i < s.length(); i++)
            hash = (R * hash + s.charAt(i)) % BUCKET_COUNT;
        return hash;
    }

    private void lockBucket(int index) {
        localBuckets[index].lock();
    }

    private void unlockBucket(int index) {
        localBuckets[index].unlock();
    }


    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    protected  <T> T routeRequestAndGet(NetworkCommand command) {
        return routeRequestAndGet(command, 1);
    }

    protected <T> T routeRequestAndGet(NetworkCommand command, int retryCount) throws OperationFailedError {
        if (retryCount < 0) {
            throw new OperationFailedError("Routing failed for command: " + command);
        }
        final Object[] resp = new Object[1];
        String msgId = GlobalConfig.getInstance().generateMsgId();
        SyncMessageListener listener = new SyncMessageListener(msgId, 1) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if(command instanceof Nack_NC) {
                    resp[0] = new SendFail_IC(command);
                    getProcessesLatch().countDown();
                } else if (command instanceof LeaderResponse_NC){
                    resp[0] = command.getPayload();
                    getProcessesLatch().countDown();
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
                }
            }

            // leader is unreachable, elect new leader and route request again.
            Address lead = tryElectLeader(true);
            return routeRequestAndGet(command.setReceiverAddress(lead), retryCount-1);

        } finally {
            detachMsgListener(listener);
        }
    }

    public Address resolveLeader() {
        return tryElectLeader(false);
    }

    protected Address tryElectLeader(boolean forceElection) throws RuntimeException {
        if(forceElection) {
            leaderUpdateLatch = new CountDownLatch(1);
        }
        if (leaderAddress.get() == null || forceElection) {
            if(isNodeTurnToElectLeader(calculateTurn(), leaderAddress.get())){
                startElection();
            }
        }
        return leaderAddress.get();
    }

    private boolean isNodeTurnToElectLeader(int currentTurn, Address prevLeaderAddr) {
        if(currentTurn <= 0) {
            return true;
        }
        try {
            leaderUpdateLatch.await(GlobalConfig.MAX_ELECTION_WAIT_SEC * currentTurn, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.error(e);
        }
        Address l = leaderAddress.get();
        if(l != null && !l.isSame(prevLeaderAddr)) {
            return false;   //leader changed return false
        }
        return isNodeTurnToElectLeader(--currentTurn, leaderAddress.get());
    }

    protected int calculateTurn() {
        List<String> addresses = new ArrayList<>();
        GlobalConfig.getInstance().getAddresses().forEach(addr -> {
            addresses.add(addr.toString());
        });
        Collections.sort(addresses);
        for (int i = 0; i < addresses.size(); i++) {
            if(addresses.get(i).equals(getAddress().toString())){
                return i;
            }
        }
        return -1;
    }

    private synchronized void updateLeader(boolean isLeader){
        setLeader(isLeader && leaderAddress.get() != null && leaderAddress.get().isSame(getAddress()));
        leaderUpdateLatch.countDown();
    }

    private synchronized void updateLeader(Address source){
        leaderAddress.set(source);
        updateLeader(source.isSame(getAddress()));
    }

    public String get(String key) {
        if(isLeader()){
            return _get(key);
        }
        Address lead = resolveLeader();
        return routeRequestAndGet(
                new ApiGet_NC()
                        .setKey(key)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(lead)
                        .setSenderAddress(getAddress()));
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
        if(isLeader()){
            return _set(key, val);
        }
        Address lead = resolveLeader();
        return routeRequestAndGet(
                new ApiSet_NC()
                        .setKey(key)
                        .setVal(val)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(lead)
                        .setSenderAddress(getAddress()));
    }
    private void setByLeader(ApiSet_NC setNc) {
        boolean isSuccess = _set(setNc.getKey(), setNc.getVal());
        sendMessage(
                new LeaderResponse_NC()
                        .setPayload(isSuccess)
                        .setReceiverAddress(setNc.getSenderAddress())
                        .setSenderAddress(getAddress())
                        .setMsgId(setNc.getMsgId())
        );
    }

    public boolean delete(String key) {
        if(isLeader()){
            return _delete(key);
        }
        Address lead = resolveLeader();
        return routeRequestAndGet(
                new ApiDelete_NC()
                        .setKey(key)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(lead)
                        .setSenderAddress(getAddress()));
    }
    private void deleteByLeader(ApiDelete_NC deleteNc) {
        boolean isDeleted = _delete(deleteNc.getKey());
        sendMessage(
                new LeaderResponse_NC()
                        .setPayload(isDeleted)
                        .setReceiverAddress(deleteNc.getSenderAddress())
                        .setSenderAddress(getAddress())
                        .setMsgId(deleteNc.getMsgId())
        );
    }

    public Set<String> iterateKeys() {
        if(isLeader()){
            return _iterateKeys();
        }
        return routeRequestAndGet(
                new ApiIterKeys_NC()
                        .setSenderId(getRoleId())
                        .setReceiverAddress(leaderAddress.get())
                        .setSenderAddress(getAddress()));
    }
    private void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        Set<String> keys = _iterateKeys();
        sendMessage(
                new LeaderResponse_NC()
                        .setPayload(keys)
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

    private void handleSendFailure(SendFail_IC sendFailIc) {
        NetworkCommand failedCommand = sendFailIc.getNetworkCommand();

        int retryCount = failedCommand.getRetryCount();
        if(retryCount > 0){
            /* Retry sending command */
            failedCommand.setRetryCount(--retryCount);
            sendMessage(failedCommand);
        } else {
            handleNodeFailure(failedCommand.getReceiverAddress());

            failedCommand.reset();  //reset retryCount etc...
            failedCommand.setReceiverAddress(leaderAddress.get());

            if(failedCommand.getRetryCount() > 0){
                handleSendFailure(new SendFail_IC(failedCommand));
            }
        }
    }

    protected void handleNodeFailure(Address failedNodeAddress) {
        super.handleNodeFailure(failedNodeAddress);
        if(failedNodeAddress.isSame(leaderAddress.get())){
            /* Handle leader failure */
            resolveLeader();
        }
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
            pleaseVote(((PleaseVote_NC) command));
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
                    .setSenderId(getRoleId())
                    .setReceiverAddress(command.getSenderAddress())
                    .setSenderAddress(getAddress())
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
}
