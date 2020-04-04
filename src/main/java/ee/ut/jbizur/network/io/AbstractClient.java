package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.tcp.custom.BlockingClientImpl;
import ee.ut.jbizur.network.io.tcp.rapidoid.RapidoidBlockingClient;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class AbstractClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final String name;
    private final Address destAddress;

    public AbstractClient(String name, Address destAddress) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(destAddress);
        this.name = name;
        this.destAddress = destAddress;
    }

    protected abstract void connect() throws IOException;
    protected abstract boolean isConnected();
    protected abstract boolean isValid();

    public void send(NetworkCommand command) throws Exception {
        Objects.requireNonNull(command);
        if (logger.isDebugEnabled()) {
            logger.debug("OUT [{}]: {}", toString(), command);
        }
        send0(command);
    }
    protected abstract void send0(NetworkCommand command) throws Exception;

    protected Future<Void> submit(Runnable r) {
        return (Future<Void>) executor.submit(r);
    }

    public String getName() {
        return name;
    }

    public Address getDestAddress() {
        return destAddress;
    }

    @Override
    public void close() {
        logger.info("closing {}", toString());
        executor.shutdown();
        try {
            executor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "Client{" +
                "name='" + name + '\'' +
                ", destAddress=" + destAddress +
                '}';
    }

    public static AbstractClient create(Class<? extends AbstractClient> clientClass, String name, Address address) {
        if (clientClass.equals(RapidoidBlockingClient.class)) {
            return new RapidoidBlockingClient(name, (TCPAddress) address);
        }
        if (clientClass.equals(BlockingClientImpl.class)) {
            return new BlockingClientImpl(name, (TCPAddress) address);
        }
        try {
            // try instantiating
            return clientClass.getConstructor(String.class, Address.class).newInstance(name, address);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnsupportedOperationException("client cannot be created, class=" + clientClass, e);
        }
    }
}
