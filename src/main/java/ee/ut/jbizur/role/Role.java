package ee.ut.jbizur.role;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.handlers.*;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.network.io.tcp.custom.BlockingServerImpl;
import ee.ut.jbizur.protocol.commands.ICommand;
import ee.ut.jbizur.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.*;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Each node is defined as a ee.ut.jbizur.role.
 */
public abstract class Role {

    private final RoleSettings settings;
    protected NetworkManager networkManager;

    protected Role(RoleSettings settings) {
        this.settings = settings;
        initRole();
    }

    protected void initRole() {
        this.networkManager = new NetworkManager(this).start();
    }

    public boolean isAddressesAlreadyRegistered() {
        return getSettings().getMemberAddresses().size() >= getSettings().getAnticipatedMemberCount();
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
            Logger.info(logMsg("End signal received: " + command));
            shutdown();
        }
    }

    public abstract void handleInternalCommand(InternalCommand command);

    protected void handleNodeFailure(Address failedNodeAddress) {
        settings.unregisterAddress(failedNodeAddress);
    }

    protected boolean pingAddress(Address address) {
        Predicate<ICommand> cdHandler = cmd -> cmd instanceof Pong_NC;
        return sendMessage(new Ping_NC(), null, cdHandler).awaitResponses();
    }

    protected void pongForPingCommand(Ping_NC pingNc) {
        sendMessage(new Pong_NC().ofRequest(pingNc));
    }

    /**
     * Sends {@link SignalEnd_NC} command to all the processes.
     */
    public void signalEndToAll() {
        sendMessageToAll(SignalEnd_NC::new, null, null);
        try {
            // wait for termination
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Logger.error(e);
        }
        shutdown();
    }

    /**
     * Sends the ee.ut.jbizur.network message to another process. Basically passes the message to the message sender service.
     * @param message the ee.ut.jbizur.network message to _send.
     */
    protected void sendMessage(NetworkCommand message) {
        message.setSenderId(getSettings().getRoleId())
                .setSenderAddress(getSettings().getAddress());
        networkManager.sendMessage(message);
    }

    protected void sendMessageToAddresses(
            Supplier<NetworkCommand> cmdSupplier,
            final Integer contextId,
            final Integer msgId,
            Set<Address> addresses
    ) {
        addresses.forEach(recvAddr -> {
            NetworkCommand cmd = cmdSupplier.get()
                    .setContextId(contextId)
                    .setMsgId(msgId)
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(recvAddr);
            sendMessage(cmd);
        });
    }

    protected void sendMessageToAll(Supplier<NetworkCommand> cmdSupplier, final Integer contextId, final Integer msgId) {
        sendMessageToAddresses(cmdSupplier, contextId, msgId, getSettings().getMemberAddresses());
    }

    protected QuorumState sendMessageToQuorum(
            Supplier<NetworkCommand> cmdSupplier,
            final Integer contextId,
            final Integer msgId,
            Predicate<ICommand> handler,
            Predicate<ICommand> countdownHandler
    ) {
        IMsgListener msgListener = new QuorumBasedMsgListener(
                getSettings().getProcessCount(),
                RoleSettings.calcQuorumSize(getSettings().getProcessCount()),
                msgId,
                networkManager.getMsgListeners()
        ).setHandler(handler).setCountdownHandler(countdownHandler);
        sendMessageToAll(cmdSupplier, contextId, msgId);
        return msgListener.getState();
    }

    protected CallbackState sendMessage(NetworkCommand command, Predicate<ICommand> handler, Predicate<ICommand> countdownHandler) {
        int msgId = command.getMsgId() == null ? IdUtils.generateId() : command.getMsgId();
        command.setMsgId(msgId);
        IMsgListener msgListener = new CallbackMsgListener(msgId, networkManager.getMsgListeners())
                .setHandler(handler)
                .setCountdownHandler(countdownHandler);
        sendMessageToAddresses(
                () -> command,
                command.getContextId(),
                msgId,
                Stream.of(command.getReceiverAddress()).collect(Collectors.toSet()));
        return msgListener.getState();
    }

    /**
     * @param modifiedAddr address modified by the {@link BlockingServerImpl} if applicable.
     */
    public void setAddress(Address modifiedAddr){
        getSettings().setAddress(modifiedAddr);
    }

    public abstract CompletableFuture start();

    public void shutdown() {
        networkManager.shutdown();
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
