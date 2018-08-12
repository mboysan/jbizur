package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

/**
 * An interface for sending messages to processes.
 */
public interface IMessageSender {
    /**
     * Sends a command/message to a process with the underlying communication stack.
     * @param command command/message to send to process.
     */
    void send(NetworkCommand command);
}
