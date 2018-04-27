package role;

import annotations.ForTestingOnly;
import config.GlobalConfig;
import datastore.bizur.Bucket;
import network.address.Address;
import network.messenger.IMessageReceiver;
import network.messenger.IMessageSender;
import network.messenger.SyncMessageListener;
import protocol.commands.NetworkCommand;
import protocol.commands.bizur.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
        int eId = ((int) System.nanoTime()) + getAddress().hashCode();
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

    /* ***************************************************************************
     * Algorithm 1 - Leader Election
     * ***************************************************************************/

    public synchronized boolean tryElectLeader(){
        startElection();
        return isLeader();
    }

    private void startElection() {
        electId.getAndIncrement();

        String msgId = GlobalConfig.getInstance().generateMsgId();

        SyncMessageListener listener = new SyncMessageListener(msgId) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if (command instanceof AckVote_NC) {
                    getProcessesLatch().countDown();
                    getAckCount().getAndIncrement();
                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof NackVote_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);

        GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
            NetworkCommand pleaseVote = new PleaseVote_NC()
                    .setElectId(electId.get())
                    .setAssocMsgId(msgId)
                    .setSenderId(getRoleId())
                    .setReceiverAddress(receiverAddress)
                    .setSenderAddress(getAddress());
            sendMessage(pleaseVote);
        });

        if(listener.waitForResponses()){
            setLeader(listener.isMajorityAcked());
        }
        detachMsgListener(listener);
    }

    private void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        int electId = pleaseVoteNc.getElectId();
        Address source = pleaseVoteNc.getSenderAddress();

        NetworkCommand vote;
        if (electId > votedElectId.get()) {
            votedElectId.set(electId);
            leaderAddress.set(source);
            vote = new AckVote_NC()
                    .setAssocMsgId(pleaseVoteNc.getAssocMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        } else if(electId == votedElectId.get() && source == leaderAddress.get()) {
            vote = new AckVote_NC()
                    .setAssocMsgId(pleaseVoteNc.getAssocMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        } else {
            vote = new NackVote_NC()
                    .setAssocMsgId(pleaseVoteNc.getAssocMsgId())
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
                    getAckCount().incrementAndGet();
                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof NackWrite_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);

        GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
            NetworkCommand replicaWrite = new ReplicaWrite_NC()
                    .setBucketView(bucket.createView())
                    .setAssocMsgId(msgId)
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
        detachMsgListener(listener);
        return isSuccess;
    }

    private void replicaWrite(ReplicaWrite_NC replicaWriteNc){
        Bucket bucket = Bucket.createBucketFromView(replicaWriteNc.getBucketView());
        Address source = replicaWriteNc.getSenderAddress();

        NetworkCommand response;
        if(bucket.getVer().getElectId() < votedElectId.get()){
            response = new NackWrite_NC()
                    .setAssocMsgId(replicaWriteNc.getAssocMsgId())
                    .setSenderId(getRoleId())
                    .setReceiverAddress(source)
                    .setSenderAddress(getAddress());
        } else {
            votedElectId.set(bucket.getVer().getElectId());
            leaderAddress.set(source);
            localBuckets[bucket.getIndex()].replaceWithBucket(bucket);

            response = new AckWrite_NC()
                    .setAssocMsgId(replicaWriteNc.getAssocMsgId())
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
                    getAckCount().incrementAndGet();
                    if(isMajorityAcked()){
                        end();
                    }
                }
                if(command instanceof NackRead_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);

        GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
            NetworkCommand replicaRead = new ReplicaRead_NC()
                    .setIndex(index)
                    .setElectId(electId.get())
                    .setSenderAddress(getAddress())
                    .setReceiverAddress(receiverAddress)
                    .setAssocMsgId(msgId);
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
        detachMsgListener(listener);
        return toReturn;
    }

    private void replicaRead(ReplicaRead_NC replicaReadNc){
        int index = replicaReadNc.getIndex();
        int electId = replicaReadNc.getElectId();
        Address source = replicaReadNc.getSenderAddress();

        if(electId < votedElectId.get()){
            NetworkCommand nackRead = new NackRead_NC()
                    .setAssocMsgId(replicaReadNc.getAssocMsgId())
                    .setSenderId(getRoleId())
                    .setSenderAddress(getAddress())
                    .setReceiverAddress(source);
            sendMessage(nackRead);
        } else {
            votedElectId.set(electId);
            leaderAddress.set(source);

            NetworkCommand ackRead = new AckRead_NC()
                    .setBucketView(localBuckets[index].createView())
                    .setAssocMsgId(replicaReadNc.getAssocMsgId())
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
                if(command instanceof NackRead_NC){
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);

        GlobalConfig.getInstance().getAddresses().forEach(receiverAddress -> {
            NetworkCommand replicaRead = new ReplicaRead_NC()
                    .setIndex(index)
                    .setElectId(electId)
                    .setSenderAddress(getAddress())
                    .setReceiverAddress(receiverAddress)
                    .setAssocMsgId(msgId);
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
        detachMsgListener(listener);
        return isSuccess;
    }

    /* ***************************************************************************
     * Algorithm 5 - Key-Value API
     * ***************************************************************************/

    public synchronized String get(String key) {
        int index = 0;  //fixme
        Bucket bucket = read(index);
        if(bucket != null){
            return bucket.getOp(key);
        }
        return null;
    }

    public synchronized boolean set(String key, String value){
        int index = 0; //fixme
        Bucket bucket = read(index);
        if (bucket != null) {
            bucket.putOp(key, value);
            return write(bucket);
        }
        return false;
    }

    public synchronized boolean delete(String key) {
        int index = 0; //fixme
        Bucket bucket = read(index);
        if(bucket != null){
            bucket.removeOp(key);
            return write(bucket);
        }
        return false;
    }

    public Set<String> iterateKeys() {
        Set<String> res = new HashSet<>();
        for (int index = 0; index < BUCKET_COUNT; index++) {
            Bucket bucket = read(index);
            if(bucket != null){
                res.addAll(bucket.getKeySet());
            }
        }
        return res;
    }

    @Override
    public void handleMessage(NetworkCommand message) {
        super.handleMessage(message);

        if (message instanceof ReplicaWrite_NC){
            replicaWrite((ReplicaWrite_NC) message);
        }
        if (message instanceof ReplicaRead_NC){
            replicaRead(((ReplicaRead_NC) message));
        }
        if (message instanceof PleaseVote_NC) {
            pleaseVote(((PleaseVote_NC) message));
        }
    }

}
