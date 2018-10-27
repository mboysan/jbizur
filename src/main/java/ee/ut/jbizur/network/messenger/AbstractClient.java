package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

import java.io.IOException;

public abstract class AbstractClient {
    protected final Role roleInstance;
    protected final boolean keepAlive;

    public AbstractClient(Role roleInstance) {
        this.roleInstance = roleInstance;
        this.keepAlive = GeneralConfig.tcpKeepAlive();
    }

    protected abstract <T> T connect(Address address) throws IOException;

    /**
     * Sends a command/message to a process with the underlying communication stack.
     * @param command command/message to _send to process.
     */
    public abstract void send(NetworkCommand command);
}
