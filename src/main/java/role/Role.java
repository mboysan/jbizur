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
import protocol.internal.InternalCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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
    protected final IMessageSender messageSender;
    /**
     * Message sender service.
     */
    protected final IMessageReceiver messageReceiver;
    /**
     * Defines if this role is the leader or not.
     */
    private final AtomicBoolean isLeader = new AtomicBoolean(false);

    /**
     * Indicates if the node is ready for registration by calling {@link GlobalConfig#registerRole(Role)}.
     */
    private CountDownLatch readyLatch;

    /**
     * @param baseAddress see {@link #myAddress}. The address may be modified by the {@link MessageReceiverImpl} after
     *                    role has been started.
     */
    Role(Address baseAddress) throws InterruptedException {
        this(baseAddress, null, null, null);
    }

    protected Role(Role rootRole) throws InterruptedException {
        this(rootRole.getAddress(), rootRole.messageSender, rootRole.messageReceiver);
    }

    protected Role(Address baseAddress, IMessageSender messageSender, IMessageReceiver messageReceiver) throws InterruptedException {
        this.myAddress = baseAddress;
        roleId = baseAddress.resolveAddressId();
        this.messageSender = messageSender == null ? new MessageSenderImpl(this) : messageSender;
        this.messageReceiver = messageReceiver == null ? new MessageReceiverImpl(this) : messageReceiver;
        this.readyLatch = null;
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
        this(baseAddress, messageSender, messageReceiver);

        this.readyLatch = readyLatch == null ? new CountDownLatch(1) : readyLatch;
        this.messageReceiver.startRecv();

        this.readyLatch.await();

        GlobalConfig.getInstance().registerRole(this);
//        pinger.pingUnreachableNodesPeriodically();
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
     * @param command the network message to handle.
     */
    public void handleNetworkCommand(NetworkCommand command){
        Logger.debug("[" + getAddress() +"] - " + command);

        boolean isHandled = false;
        String assocMsgId = command.getMsgId();
        if(assocMsgId != null){
            SyncMessageListener listener = syncMessageListeners.get(assocMsgId);
            if(listener != null){
                listener.handleMessage(command);
                isHandled = true;
            }
        }

        if(!isHandled){
            if(command instanceof Connect_NC){
                NetworkCommand connectOK = new ConnectOK_NC()
                        .setSenderAddress(getAddress())
                        .setReceiverAddress(command.getSenderAddress());
                sendMessage(connectOK);
            }
            if(command instanceof ConnectOK_NC){
                GlobalConfig.getInstance().registerAddress(command.getSenderAddress(), this);
            }
        }
    }

    public abstract void handleInternalCommand(InternalCommand command);

    protected void handleNodeFailure(Address failedNodeAddress) {
        GlobalConfig.getInstance().unregisterAddress(failedNodeAddress, this);
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
        return isLeader.get();
    }

    public void setLeader(boolean leader) {
        isLeader.set(leader);
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
