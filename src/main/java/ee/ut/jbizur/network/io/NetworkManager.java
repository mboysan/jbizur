package ee.ut.jbizur.network.io;

import ee.ut.jbizur.common.ResourceCloser;
import ee.ut.jbizur.common.config.Conf;
import ee.ut.jbizur.common.protocol.address.Address;
import ee.ut.jbizur.common.protocol.address.MulticastAddress;
import ee.ut.jbizur.common.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.network.handlers.BaseListener;
import ee.ut.jbizur.network.io.udp.Multicaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class NetworkManager implements AutoCloseable, ResourceCloser {

    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);

    private volatile boolean isRunning = false;

    private final Map<Address, ClientPool> clientPools = new ConcurrentHashMap<>();
    private AbstractServer server;
    private Multicaster multicaster;

    final String name;
    final Address serverAddress;
    final Consumer<InternalCommand> internalCommandConsumer;
    final Consumer<NetworkCommand> networkCommandConsumer;

    public NetworkManager(
            String name,
            Address serverAddress,
            Consumer<InternalCommand> internalCommandConsumer,
            Consumer<NetworkCommand> networkCommandConsumer) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(serverAddress);
        Objects.requireNonNull(internalCommandConsumer);

        this.name = name;
        this.serverAddress = serverAddress;
        this.internalCommandConsumer = internalCommandConsumer;
        this.networkCommandConsumer = networkCommandConsumer;
    }

    public NetworkManager start() throws UnknownHostException {
        Objects.requireNonNull(serverAddress);
        Objects.requireNonNull(networkCommandConsumer);

        this.server = createServer();
        BaseListener baseListener = new BaseListener(networkCommandConsumer);
        server.add(0, baseListener);
        server.start();

        this.multicaster = createMulticaster();
        if (multicaster != null) {
            multicaster.add(0, baseListener);
            multicaster.start();
        }

        isRunning = true;
        return this;
    }

    @Override
    public void close() {
        isRunning = false;
        closeResources(logger, multicaster, server);
        closeResources(logger, clientPools.values());
    }

    protected Multicaster createMulticaster() throws UnknownHostException {
        if (Conf.get().network.multicast.enabled) {
            MulticastAddress multicastAddress = new MulticastAddress(Conf.get().network.multicast.address);
            return new Multicaster("multicaster-" + name, multicastAddress);
        }
        return null;
    }

    protected AbstractServer createServer() {
        try {
            return AbstractServer.create(
                    (Class<? extends AbstractServer>) Class.forName(Conf.get().network.server),
                    "server-" + name,
                    serverAddress
            );
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
        throw new IllegalArgumentException("server could not be initiated");
    }

    private void validateAction() {
        if (!isRunning) {
            throw new IllegalStateException("Network manager is not running");
        }
    }

    public void multicast(NetworkCommand command) {
        validateAction();
        Objects.requireNonNull(multicaster);
        multicaster.multicast(command);
    }

    public void multicastAtFixedRate(NetworkCommand command) {
        validateAction();
        Objects.requireNonNull(multicaster);
        multicaster.multicastAtFixedRate(command);
    }

    public void subscribe(int correlationId, Predicate<NetworkCommand> listener) {
        validateAction();
        Objects.requireNonNull(listener);
        server.add(correlationId, listener);
    }

    public void publish(Supplier<NetworkCommand> commandSupplier, Set<Address> toAddresses) {
        toAddresses.forEach(address -> {
            NetworkCommand cmd = commandSupplier.get()
                    .setReceiverAddress(address)
                    .setSenderAddress(serverAddress);
            try {
                send(cmd);
            } catch (IOException e) {
                logger.error("[{}] error publishing command={} to address={}", toString(), cmd, address);
            }
        });
    }

    public void send(NetworkCommand message) throws IOException {
        ClientPool clientPool = clientPools.computeIfAbsent(message.getReceiverAddress(), ClientPool::new);
        AbstractClient client = clientPool.checkOut();
        try {
            client.send(message.setSenderAddress(serverAddress));
        } catch (Exception e) {
            logger.error("[{}] error sending command={}", toString(), message, e);
            throw new IOException(e);
        } finally {
            clientPool.checkIn(client);
        }
    }

    @Override
    public String toString() {
        return "NetworkManager{" +
                "isRunning=" + isRunning +
                ", name='" + name + '\'' +
                ", serverAddress=" + serverAddress +
                '}';
    }
}
