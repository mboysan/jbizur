package ee.ut.jbizur.role;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.LoggerConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.MessageProcessor;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.network.messenger.tcp.custom.BlockingServerImpl;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.*;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import org.pmw.tinylog.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each node is defined as a ee.ut.jbizur.role.
 */
public abstract class Role {

    protected final Map<Integer, SyncMessageListener> syncMessageListeners = new ConcurrentHashMap<>();

    private final RoleSettings settings;
    protected final MessageProcessor messageProcessor;

    protected Role(RoleSettings settings) throws InterruptedException {
        this(settings, null);
    }

    @ForTestingOnly
    protected Role(RoleSettings settings, MessageProcessor messageProcessor) throws InterruptedException {
        this.settings = settings;
        this.messageProcessor = messageProcessor == null ? new MessageProcessor(this) : messageProcessor;
        initRole();
    }

    protected void initRole() {
        if (messageProcessor != null && messageProcessor.getRole() != null) {
            this.messageProcessor.start();
        }
    }

    public boolean isAddressesAlreadyRegistered() {
        return getSettings().getMemberAddresses().size() == getSettings().getAnticipatedMemberCount();
    }

    public boolean checkNodesDiscovered() {
        return RoleValidation.checkStateAndWarn(
                isAddressesAlreadyRegistered(),
                logMsg("Searching for other nodes in the system..."));
    }

    /**
     * Handles the provided ee.ut.jbizur.network message. Each ee.ut.jbizur.role implements its own message handling mechanism.
     * @param command the ee.ut.jbizur.network message to handle.
     */
    public void handleNetworkCommand(NetworkCommand command){
        if (LoggerConfig.isDebugEnabled()) {
            Logger.debug("IN " + logMsg(command.toString()));
        }

        boolean isHandled = false;
        Integer assocMsgId = command.getMsgId();
        if (assocMsgId != null) {
            SyncMessageListener listener = syncMessageListeners.get(assocMsgId);
            if(listener != null){
                listener.handleMessage(command);
                isHandled = true;
            }
        }

        if(!isHandled){
            if(command instanceof Connect_NC){
                NetworkCommand connectOK = new ConnectOK_NC()
                        .setSenderId(getSettings().getRoleId())
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
            if (command instanceof Ping_NC) {
                pongForPingCommand((Ping_NC) command);
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

    protected boolean pingAddress(Address address) {
        SyncMessageListener listener = SyncMessageListener.build()
                .withTotalProcessCount(1)
                .registerHandler(Pong_NC.class, (cmd, lst) -> {
                    lst.getPassedObjectRef().set(true);
                    lst.end();
                });
        attachMsgListener(listener);
        try {
            NetworkCommand pingNC = new Ping_NC()
                    .setMsgId(listener.getMsgId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(address)
                    .setSenderId(getSettings().getRoleId());
            if (LoggerConfig.isDebugEnabled()) {
                listener.withDebugInfo(logMsg("pingAddress: " + pingNC));
            }
            sendMessage(pingNC);
            if (listener.waitForResponses()) {
                return (boolean) listener.getPassedObjectRef().get();
            }
        } finally {
            detachMsgListener(listener);
        }
        return false;
    }

    protected void pongForPingCommand(Ping_NC pingNc) {
        NetworkCommand pongNC = new Pong_NC()
                .setMsgId(pingNc.getMsgId())
                .setReceiverAddress(pingNc.getSenderAddress())
                .setSenderAddress(getSettings().getAddress())
                .setSenderId(getSettings().getRoleId());
        sendMessage(pongNC);
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
     * @param message the ee.ut.jbizur.network message to _send.
     */
    protected void sendMessage(NetworkCommand message) {
        if (LoggerConfig.isDebugEnabled()) {
            Logger.debug("OUT " + logMsg(message.toString()));
        }
        messageProcessor.getClient().send(message);
    }

    /**
     * @param modifiedAddr address modified by the {@link BlockingServerImpl} if applicable.
     */
    public void setAddress(Address modifiedAddr){
        getSettings().setAddress(modifiedAddr);
    }

    public abstract CompletableFuture start();

    public void shutdown() {
        messageProcessor.shutdown();
    }

    protected void attachMsgListener(SyncMessageListener listener){
        syncMessageListeners.putIfAbsent(listener.getMsgId(), listener);
    }

    protected void detachMsgListener(SyncMessageListener listener){
        syncMessageListeners.remove(listener.getMsgId());
    }

    public RoleSettings getSettings() {
        return settings;
    }

    public String logMsg(String msg) {
        return String.format("[%s] %s", getSettings().getRoleId(), msg);
    }

    @Override
    public String toString() {
        return "Role{" +
                "roleId='" + getSettings().getRoleId() + '\'' +
                ", myAddress=" + getSettings().getAddress() +
                '}';
    }
}
