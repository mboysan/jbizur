package role;

import annotations.ForTestingOnly;
import config.GlobalConfig;
import network.address.Address;
import network.messenger.*;
import org.pmw.tinylog.Logger;
import protocol.commands.NetworkCommand;
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

    private final Map<String, SyncMessageListener> syncMessageListeners = new ConcurrentHashMap<>();

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
    private final IMessageSender messageSender;
    /**
     * Message sender service.
     */
    private final IMessageReceiver messageReceiver;
    /**
     * Defines if this role is the leader or not.
     */
    private boolean isLeader = false;

    /**
     * Indicates if the node is ready for registration by calling {@link GlobalConfig#registerRole(Role)}.
     */
    private final CountDownLatch readyLatch;

    /**
     * @param baseAddress see {@link #myAddress}. The address may be modified by the {@link MessageReceiverImpl} after
     *                    role has been started.
     */
    Role(Address baseAddress) throws InterruptedException {
        this(baseAddress, null, null, null);
    }

    /**
     * Designed to be used by the unit testing framework, hence protected access.
     *
     * @param baseAddress see {@link #myAddress}. The address may be modified by the {@link MessageReceiverImpl} after
     *                    role has been started.
     * @param messageSender message sender service.
     * @param messageReceiver message receiver service.
     */
    @ForTestingOnly
    protected Role(Address baseAddress,
                   IMessageSender messageSender,
                   IMessageReceiver messageReceiver,
                   CountDownLatch readyLatch) throws InterruptedException
    {
        this.myAddress = baseAddress;
        roleId = baseAddress.resolveAddressId();

        if(messageSender == null){
            messageSender = new MessageSenderImpl();
        }
        if(messageReceiver == null){
            messageReceiver = new MessageReceiverImpl(this);
        }
        this.messageSender = messageSender;
        this.messageReceiver = messageReceiver;
        this.messageReceiver.startRecv();

        this.readyLatch = readyLatch == null ? new CountDownLatch(1) : readyLatch;
        this.readyLatch.await();

        GlobalConfig.getInstance().registerRole(this);
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

        boolean isHandled = false;
        String assocMsgId = message.getAssocMsgId();
        if(assocMsgId != null){
            SyncMessageListener listener = syncMessageListeners.get(assocMsgId);
            if(listener != null){
                isHandled = true;
                listener.handleMessage(message);
                message.setHandled(true);   //TODO: might need to find a better solution to this.
            }
        }

        if(!isHandled){
            if(message instanceof Connect_NC){
                NetworkCommand connectOK = new ConnectOK_NC()
                        .setSenderAddress(getAddress())
                        .setReceiverAddress(message.getSenderAddress());
                sendMessage(connectOK);
            }
            if(message instanceof ConnectOK_NC){
                GlobalConfig.getInstance().registerAddress(message.getSenderAddress(), this);
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
     * @param modifiedAddr address modified by the {@link MessageReceiverImpl} if applicable.
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

    protected void attachMsgListener(SyncMessageListener listener){
        syncMessageListeners.putIfAbsent(listener.getMsgId(), listener);
    }

    public void detachMsgListener(SyncMessageListener listener){
        syncMessageListeners.remove(listener.getMsgId());
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
