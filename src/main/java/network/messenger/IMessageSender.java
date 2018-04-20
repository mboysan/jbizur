package network.messenger;

import protocol.commands.NetworkCommand;

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
