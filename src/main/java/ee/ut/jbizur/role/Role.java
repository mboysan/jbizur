package ee.ut.jbizur.role;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.handlers.CallbackListener;
import ee.ut.jbizur.network.handlers.QuorumListener;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.*;
import ee.ut.jbizur.util.IdUtils;
import ee.ut.jbizur.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Each node is defined as a ee.ut.jbizur.role.
 */
public abstract class Role implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Role.class);

    private final RoleSettings settings;
    protected NetworkManager networkManager;

    protected Role(RoleSettings settings) throws IOException {
        this.settings = settings;
        this.settings.setInternalCommandConsumer(this::handle);
        initRole();
    }

    protected void initRole() throws IOException {
        Address address = settings.getAddress();
        if (address instanceof TCPAddress) {
            if (((TCPAddress) address).getPortNumber() == 0) {
                ((TCPAddress) address).setPortNumber(NetUtil.findOpenPort());
            }
        }
        String nmName = "nm-" + settings.getRoleId();
        this.networkManager = new NetworkManager(nmName, address, this::handle, this::handle).start();

        if (settings.isMultiCastEnabled()) {
            networkManager.multicastAtFixedRate(new Connect_NC()
                    .setSenderId(settings.getRoleId())
                    .setSenderAddress(settings.getAddress())
                    .setNodeType("member"));
        }
    }

    protected boolean isAddressesAlreadyRegistered() {
        return getSettings().getMemberAddresses().size() >= getSettings().getAnticipatedMemberCount();
    }

    protected boolean checkNodesDiscovered() {
        return RoleValidation.checkStateAndWarn(
                isAddressesAlreadyRegistered(),
                logMsg("Searching for other nodes in the system..."));
    }

    /**
     * Handles the provided ee.ut.jbizur.network message. Each ee.ut.jbizur.role implements its own message handling mechanism.
     * @param nc the ee.ut.jbizur.network message to handle.
     */
    protected void handle(NetworkCommand nc){
        if(nc instanceof Connect_NC){
            NetworkCommand connectOK = new ConnectOK_NC()
                    .setSenderId(getSettings().getRoleId())
                    .setSenderAddress(getSettings().getAddress())
                    .setReceiverAddress(nc.getSenderAddress())
                    .setNodeType("member");
            try {
                send(connectOK);
            } catch (IOException e) {
                logger.error(logMsg(e.getMessage()), e);
            }
        }
        if(nc instanceof ConnectOK_NC){
            if (nc.getNodeType().equals("member")) {
                settings.registerAddress(nc.getSenderAddress());
            }
        }
        if (nc instanceof Ping_NC) {
            // send pong
            try {
                send(new Pong_NC().ofRequest(nc));
            } catch (IOException e) {
                logger.error(logMsg(e.getMessage()), e);
            }
        }
        if (nc instanceof SignalEnd_NC) {
            logger.info(logMsg("End signal received: " + nc));
            close();
        }
    }

    protected abstract void handle(InternalCommand ic);

    protected void handleNodeFailure(Address failedNodeAddress) {
        settings.unregisterAddress(failedNodeAddress);
    }

    public BooleanSupplier receive(int correlationId, Consumer<NetworkCommand> commandConsumer) {
        CallbackListener cl = new CallbackListener(commandConsumer);
        networkManager.subscribe(correlationId, cl);
        return () -> cl.await(Conf.get().network.responseTimeoutSec, TimeUnit.SECONDS);
    }

    public void send(NetworkCommand command) throws IOException {
        Objects.requireNonNull(command);
        networkManager.send(command);
    }

    public NetworkCommand sendRecv(NetworkCommand command) throws IOException {
        AtomicReference<NetworkCommand> response = new AtomicReference<>();
        BooleanSupplier isComplete = receive(command.getCorrelationId(), response::set);
        send(command);
        return isComplete.getAsBoolean() ? response.get() : null;
    }


    public BooleanSupplier subscribe(int correlationId, Predicate<NetworkCommand> cmdPredicate) {
        Objects.requireNonNull(cmdPredicate);
        QuorumListener qc = new QuorumListener(
                getSettings().getProcessCount(),
                RoleSettings.calcQuorumSize(getSettings().getProcessCount()),
                cmdPredicate
        );
        networkManager.subscribe(correlationId, qc);
        return () -> qc.await(Conf.get().network.responseTimeoutSec, TimeUnit.SECONDS)  // wait for responses
                && qc.isMajorityAcked();    // return majority response
    }

    public void publish(Supplier<NetworkCommand> commandSupplier) {
        Objects.requireNonNull(commandSupplier);
        networkManager.publish(commandSupplier, getSettings().getMemberAddresses());
    }


    protected boolean ping(Address address) throws IOException {
        NetworkCommand resp = sendRecv(
                new Ping_NC()
                        .setCorrelationId(IdUtils.generateId())
                        .setSenderAddress(getSettings().getAddress())
                        .setReceiverAddress(address));
        return resp instanceof Pong_NC;
    }

    /**
     * Sends {@link SignalEnd_NC} command to all the processes.
     */
    public void signalEndToAll() {
        try {
            publish(SignalEnd_NC::new);
            // wait for termination
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } finally {
            close();
        }
    }

    public abstract CompletableFuture start();

    @Override
    public void close() {
        networkManager.close();
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
