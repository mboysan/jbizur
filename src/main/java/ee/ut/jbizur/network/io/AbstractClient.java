package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.io.IOException;

public abstract class AbstractClient {
    protected final NetworkManager networkManager;
    protected final boolean keepAlive;

    public AbstractClient(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.keepAlive = Conf.get().network.tcp.keepalive;
    }

    protected abstract <T> T connect(Address address) throws IOException;

    /**
     * Sends a command/message to a process with the underlying communication stack.
     * @param command command/message to _send to process.
     */
    public abstract void send(NetworkCommand command);

    public abstract void shutdown();
}
