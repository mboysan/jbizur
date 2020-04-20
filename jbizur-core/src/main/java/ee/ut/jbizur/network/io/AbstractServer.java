package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.MulticastAddress;
import ee.ut.jbizur.protocol.address.TCPAddress;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import ee.ut.jbizur.network.handlers.Listeners;
import ee.ut.jbizur.network.io.tcp.custom.BlockingServerImpl;
import ee.ut.jbizur.network.io.tcp.rapidoid.RapidoidServer;
import ee.ut.jbizur.network.io.udp.Multicaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static ee.ut.jbizur.common.util.LambdaUtil.runnable;

public abstract class AbstractServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    private volatile boolean isRunning = true;

//    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final String name;
    private final Address serverAddress;
    private final Listeners listeners = new Listeners();

    public AbstractServer(String name, Address serverAddress) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(serverAddress);

        this.name = name;
        this.serverAddress = serverAddress;
    }

    public void start() {
        logger.info("starting {}", toString());
        isRunning = true;
    }

    public void add(int correlationId, Predicate<NetworkCommand> listener) {
        listeners.add(correlationId, listener);
    }

    protected void recv(NetworkCommand command) {
        validateAction();
        if (logger.isDebugEnabled()) {
            logger.debug("IN (sync) [{}]: {}", toString(), command);
        }
        listeners.handle(command);
    }

    protected void recvAsync(NetworkCommand command) {
        validateAction();
        if (logger.isDebugEnabled()) {
            logger.debug("IN [{}]: {}", toString(), command);
        }
        submit(runnable(() -> listeners.handle(command)));
    }

    protected void submit(Runnable r) {
        executor.submit(r);
    }

    protected void validateAction() {
        if (!isRunning) {
            throw new IllegalStateException("server not running");
        }
    }

    @Override
    public void close() {
        logger.info("closing {}", toString());
        isRunning = false;
        executor.shutdown();
        try {
            executor.awaitTermination(CoreConf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String getName() {
        return name;
    }

    public Address getServerAddress() {
        return serverAddress;
    }

    @Override
    public String toString() {
        return "Server{" +
                "running=" + isRunning +
                ", name='" + name + '\'' +
                ", addr=" + serverAddress +
                '}';
    }

    public static AbstractServer create(Class<? extends AbstractServer> serverClass, String name, Address serverAddress) {
        if (serverClass.equals(BlockingServerImpl.class)) {
            return new BlockingServerImpl(name, (TCPAddress) serverAddress);
        }
        if (serverClass.equals(RapidoidServer.class)) {
            return new RapidoidServer(name, (TCPAddress) serverAddress);
        }
        if (serverClass.equals(Multicaster.class)) {
            return new Multicaster(name, (MulticastAddress) serverAddress);
        }
        try {
            // try instantiating
            return serverClass.getConstructor(String.class, Address.class).newInstance(name, serverAddress);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnsupportedOperationException("server cannot be created, class="+ serverClass, e);
        }
    }
}
