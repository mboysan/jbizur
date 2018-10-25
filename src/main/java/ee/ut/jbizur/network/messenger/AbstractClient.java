package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.Future;

public abstract class AbstractClient {
    protected Role roleInstance;

    public AbstractClient(Role roleInstance) {
        this.roleInstance = roleInstance;
    }

    /**
     * Sends a command/message to a process with the underlying communication stack.
     * @param command command/message to send to process.
     */
    public abstract void send(NetworkCommand command);
}
