package ee.ut.jbizur.role;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.*;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.ConnectOK_NC;
import ee.ut.jbizur.protocol.commands.ping.Connect_NC;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import org.pmw.tinylog.Logger;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Each node is defined as a ee.ut.jbizur.role.
 */
public abstract class Role {

    protected final Map<String, SyncMessageListener> syncMessageListeners = new ConcurrentHashMap<>();

    protected ScheduledExecutorService multicastExecutor = Executors.newScheduledThreadPool(1);
    /**
     * Message sender service.
     */
    protected final IMessageSender messageSender;
    /**
     * Message sender service.
     */
    protected final IMessageReceiver messageReceiver;
    /**
     * Defines if this ee.ut.jbizur.role is the leader or not.
     */
    private final AtomicBoolean isLeader = new AtomicBoolean(false);

    private final CountDownLatch readyLatch;

    private final RoleSettings settings;
    protected final Multicaster multicaster;

    protected Role(RoleSettings settings) throws InterruptedException {
        this(settings, null, null, null, null);
    }

    @ForTestingOnly
    protected Role(RoleSettings settings, Multicaster multicaster, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        this.settings = settings;
        this.multicaster = multicaster == null ? new Multicaster(settings.getMulticastAddress(), this) : multicaster;
        this.messageSender = messageSender == null ? new MessageSenderImpl(this) : messageSender;
        this.messageReceiver = messageReceiver == null ? new MessageReceiverImpl(this) : messageReceiver;
        this.readyLatch = readyLatch == null ? new CountDownLatch(1) : readyLatch;

        this.messageReceiver.startRecv();
        this.readyLatch.await();

        initMulticast();
    }

    protected void initMulticast() {
        if (isAddressesAlreadyRegistered()) {
            return;
        }
        multicaster.startRecv();
        multicastExecutor.scheduleAtFixedRate(() -> {
            if (!checkNodesDiscovered()) {
                multicaster.multicast(
                        new Connect_NC()
                                .setSenderAddress(settings.getAddress())
                                .setNodeType("member")
                );
            }
        }, 0, NodeConfig.getMulticastIntervalMs(), TimeUnit.MILLISECONDS);
    }

    protected boolean isAddressesAlreadyRegistered() {
        return getSettings().getMemberAddresses().size() > 0;
    }

    protected boolean checkNodesDiscovered() {
        return RoleValidation.checkStateAndWarn(
                getSettings().getAnticipatedMemberCount() == getSettings().getMemberAddresses().size(),
                "Nodes did not find each other yet.");
    }

    /**
     * Handles the provided ee.ut.jbizur.network message. Each ee.ut.jbizur.role implements its own message handling mechanism.
     * @param command the ee.ut.jbizur.network message to handle.
     */
    public void handleNetworkCommand(NetworkCommand command){
        Logger.debug("[" + getSettings().getAddress() +"] - " + command);

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
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(command.getSenderAddress())
                        .setNodeType("member");
                sendMessage(connectOK);
            }
            if(command instanceof ConnectOK_NC){
                if (command.getNodeType().equals("member")) {
                    settings.registerAddress(command.getSenderAddress());
                }
            }
            if (command instanceof SignalEnd_NC) {
                shutdown();
            }
        }
    }

    public abstract void handleInternalCommand(InternalCommand command);

    protected void handleNodeFailure(Address failedNodeAddress) {
        settings.unregisterAddress(failedNodeAddress);
    }

    /**
     * Sends {@link SignalEnd_NC} command to all the processes.
     */
    public void signalEndToAll() {
        for (Address receiverAddress : getSettings().getMemberAddresses()) {
            NetworkCommand signalEnd = new SignalEnd_NC()
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(receiverAddress)
                    .setSenderAddress(getSettings().getAddress());
            sendMessage(signalEnd);
        }
    }

    /**
     * Sends the ee.ut.jbizur.network message to another process. Basically passes the message to the message sender service.
     * @param message the ee.ut.jbizur.network message to send.
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
        getSettings().setAddress(modifiedAddr);
    }

    /**
     * Indicates the ee.ut.jbizur.role is ready for further state changes.
     */
    public void setReady(){
        readyLatch.countDown();
    }

    public abstract CompletableFuture start();

    public void shutdown() {
        messageReceiver.shutdown();
        multicaster.shutdown();
        multicastExecutor.shutdown();
    }

    protected void attachMsgListener(SyncMessageListener listener){
        syncMessageListeners.putIfAbsent(listener.getMsgId(), listener);
    }

    public void detachMsgListener(SyncMessageListener listener){
        syncMessageListeners.remove(listener.getMsgId());
    }

    public RoleSettings getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return "Role{" +
                "roleId='" + getSettings().getRoleId() + '\'' +
                ", myAddress=" + getSettings().getAddress() +
                ", isLeader=" + isLeader +
                '}';
    }
}
