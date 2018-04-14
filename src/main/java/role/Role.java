package role;

import config.GlobalConfig;
import network.address.Address;
import network.messenger.MessageReceiver;
import network.messenger.MessageSender;
import org.pmw.tinylog.Logger;
import protocol.commands.NetworkCommand;
import protocol.commands.SequenceNumber;
import protocol.commands.ping.ConnectOK_NC;
import protocol.commands.ping.Connect_NC;
import protocol.commands.ping.SignalEnd_NC;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Each node is defined as a role.
 */
public abstract class Role {

    /**
     * Id of the role
     */
    private final String roleId;
    /**
     * The address of the role.
     */
    private Address myAddress;
    /**
     * Message sender service.
     */
    private final MessageSender messageSender;
    /**
     * Defines if this role is the leader or not.
     */
    private boolean isLeader = false;

    /**
     * Indicates if the node is ready for registration by calling {@link GlobalConfig#registerRole(Role)}.
     */
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    private final Map<String, CountDownLatch> messageSyncMap = new ConcurrentHashMap<>();

    /**
     * @param baseAddress see {@link #myAddress}. The address may be modified by the {@link MessageReceiver} after
     *                    role has been started.
     */
    Role(Address baseAddress) throws InterruptedException {
        this.myAddress = baseAddress;
        roleId = baseAddress.resolveAddressId();

        this.messageSender = new MessageSender();
        recvAsync();

        readyLatch.await();

        GlobalConfig.getInstance().registerRole(this);
    }

    /**
     * Starts the role. Basically starts the message receiver service.
     */
    private void recvAsync() {
        new MessageReceiver(this);
    }

    /**
     * @return see {@link #myAddress}
     */
    public Address getAddress() {
        return myAddress;
    }

    /**
     * @return see {@link #roleId}
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Handles the provided network message. Each role implements its own message handling mechanism.
     * @param message the network message to handle.
     */
    public void handleMessage(NetworkCommand message){
        Logger.debug("[" + getAddress() +"] - " + message);

        countdownMsgLatch(message);

        if(message instanceof Connect_NC){
            NetworkCommand connectOK = new ConnectOK_NC()
                    .setSenderAddress(getAddress())
                    .setReceiverAddress(message.resolveSenderAddress());
            sendMessage(connectOK);
        }
        if(message instanceof ConnectOK_NC){
            GlobalConfig.getInstance().registerAddress(message.resolveSenderAddress(), this);
        }
    }

    protected CountDownLatch prepareForSync(SequenceNumber sequenceNumber, int syncCount) {
        CountDownLatch msgLatch = messageSyncMap.get(sequenceNumber.toString());
        if(msgLatch == null){
            msgLatch = new CountDownLatch(syncCount);
            messageSyncMap.put(sequenceNumber.toString(), msgLatch);
        }
        return msgLatch;
    }

    protected void countdownMsgLatch(NetworkCommand message) {
        SequenceNumber assocSeqNum = message.getAssocMsgId();
        if(assocSeqNum != null) {
            CountDownLatch msgLatch = messageSyncMap.get(assocSeqNum.toString());
            if(msgLatch != null){
                msgLatch.countDown();
            }
        }
    }

    /**
     * Sends {@link SignalEnd_NC} command to all the processes.
     */
    public void signalEndToAll() {
        for (Address receiverAddress : GlobalConfig.getInstance().getAddresses()) {
            NetworkCommand signalEnd = new SignalEnd_NC()
                    .setSenderId(getRoleId())
                    .setReceiverAddress(receiverAddress)
                    .setSenderAddress(getAddress());
            sendMessage(signalEnd);
        }
    }

    /**
     * Sends the network message to another process. Basically passes the message to the message sender service.
     * @param message the network message to send.
     */
    protected void sendMessage(NetworkCommand message) {
        messageSender.send(message);
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }

    /**
     * @param modifiedAddr address modified by the {@link MessageReceiver} if applicable.
     */
    public void setAddress(Address modifiedAddr){
        this.myAddress = modifiedAddr;
    }

    /**
     * Indicates the role is ready for further state changes.
     */
    public void setReady(){
        readyLatch.countDown();
    }

    @Override
    public String toString() {
        return "Role{" +
                "roleId='" + roleId + '\'' +
                ", myAddress=" + myAddress +
                ", isLeader=" + isLeader +
                '}';
    }
}
