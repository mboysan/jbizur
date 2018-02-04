package network.communication;

import protocol.commands.GenericCommand;

public interface IMessageHandler {
    void sendCommand(GenericCommand command);
}
