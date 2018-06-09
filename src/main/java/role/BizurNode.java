package role;

import annotations.ForTestingOnly;
import config.GlobalConfig;
import datastore.bizur.Bucket;
import network.address.Address;
import network.messenger.IMessageReceiver;
import network.messenger.IMessageSender;
import network.messenger.SyncMessageListener;
import org.pmw.tinylog.Logger;
import protocol.commands.common.Nack_NC;
import protocol.commands.NetworkCommand;
import protocol.commands.bizur.*;
import protocol.internal.InternalCommand;
import protocol.internal.NodeDead_IC;
import protocol.internal.SendFail_IC;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BizurNode extends Role {

    private static final int BUCKET_COUNT = 1;

    private AtomicInteger electId;
    private AtomicInteger votedElectId;
    private AtomicReference<Address> leaderAddress;

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

    private void initNode() {
        int eId = Math.abs((int) System.currentTimeMillis() + getRoleId().hashCode());
        electId = new AtomicInteger(eId);
        votedElectId = new AtomicInteger(0);
        leaderAddress = new AtomicReference<>(null);
    }

    private void initBuckets(int count) {
        localBuckets = new Bucket[count];
        for (int i = 0; i < count; i++) {
            Bucket b = new Bucket()
                    .setIndex(i);
            localBuckets[i] = b;
        }
    }

    private void updateLeader(boolean isLeader){
        setLeader(isLeader && (leaderAddress.get() != null && leaderAddress.get().isSame(getAddress())));
    }

    private void updateLeader(Address source){
        if(source != null) {
            leaderAddress.getAndUpdate(address -> {
                setLeader(source.isSame(getAddress()));
                return source;
            });
        }
    }

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    private void startElection() {
        electId.getAndIncrement();

        String msgId = GlobalConfig.getInstance().generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if (command instanceof AckVote_NC) {
                    getProcessesLatch().countDown();
                    incrementAckCount();
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

    private synchronized void pleaseVote(PleaseVote_NC pleaseVoteNc) {
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
                    getProcessesLatch().countDown();
                    incrementAckCount();
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
                    setLeader(false);
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
                    getProcessesLatch().countDown();
                    incrementAckCount();
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
                    setLeader(false);
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
                    getProcessesLatch().countDown();
                    incrementAckCount();

                    AckRead_NC ackRead = ((AckRead_NC) command);
                    Bucket bucket = Bucket.createBucketFromView(ackRead.getBucketView());

                    if(maxVerBucket[0] == null){
                        maxVerBucket[0] = bucket;
                    } else {
                        if(bucket.getVer().compareTo(maxVerBucket[0].getVer()) > 0){
                            maxVerBucket[0] = bucket;
                        }
                    }

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
                    setLeader(false);
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

    private synchronized String _get(String key) {
        int index = 0;  //fixme
        Bucket bucket = read(index);
        if(bucket != null){
            return bucket.getOp(key);
        }
        return null;
    }

    private synchronized boolean _set(String key, String value){
        int index = 0; //fixme
        Bucket bucket = read(index);
        if (bucket != null) {
            bucket.putOp(key, value);
            return write(bucket);
        }
        return false;
    }

    private synchronized boolean _delete(String key) {
        int index = 0; //fixme
        Bucket bucket = read(index);
        if(bucket != null){
            bucket.removeOp(key);
            return write(bucket);
        }
        return false;
    }

    private synchronized Set<String> _iterateKeys() {
        Set<String> res = new HashSet<>();
        for (int index = 0; index < BUCKET_COUNT; index++) {
            Bucket bucket = read(index);
            if(bucket != null){
                res.addAll(bucket.getKeySet());
            }
        }
        return res;
    }


    /* ***************************************************************************
     * Public API
     * ***************************************************************************/

    private <T> T routeRequestAndGet(NetworkCommand command) {
        final Object[] resp = new Object[1];
        String msgId = GlobalConfig.getInstance().generateMsgId();
        SyncMessageListener listener = new SyncMessageListener(msgId, 1) {
            @Override
            public void handleMessage(NetworkCommand command) {
                resp[0] = (T) command.getPayload();
                getProcessesLatch().countDown();
            }
        };
        attachMsgListener(listener);
        try{

        command.setMsgId(msgId);

        sendMessage(command);
            if(listener.waitForResponses()){
                return (T) resp[0];
            } else {
                startElection();
                routeRequestAndGet(command.setReceiverAddress(leaderAddress.get()));
            }
        } finally {
            detachMsgListener(listener);
        }
        return null;
    }

    public Address tryElectLeader() throws RuntimeException {
        int retryCount = 5;
        if (leaderAddress.get() == null) {
            for (int i = 0; i < retryCount; i++) {
                startElection();
                if (leaderAddress.get() != null) {
                    return leaderAddress.get();
                }
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Logger.error(e);
                }
            }
            throw new RuntimeException("Leader could not be elected.");
        }
        return leaderAddress.get();
    }

    public String get(String key) {
        if(isLeader()){
            return _get(key);
        }
        Address lead = tryElectLeader();
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
                new NetworkCommand()
                        .setPayload(val)
                        .setReceiverAddress(getNc.getSenderAddress())
                        .setMsgId(getNc.getMsgId())
        );
    }

    public boolean set(String key, String val) {
        if(isLeader()){
            return _set(key, val);
        }
        Address lead = tryElectLeader();
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
                new NetworkCommand()
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
        Address lead = tryElectLeader();
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
                new NetworkCommand()
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
                new NetworkCommand()
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
        handleNetworkCommand(
                new Nack_NC().setMsgId(failedCommand.getMsgId())
        );
        pinger.registerUnreachableAddress(failedCommand.getReceiverAddress());
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
            tryElectLeader();
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

    @Override
    public void shutdown() {
        pinger.shutdown();
    }
}
