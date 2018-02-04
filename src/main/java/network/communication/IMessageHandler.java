package network.communication;

import protocol.commands.ICommand;

public interface IMessageHandler {
    void sendCommand(ICommand command);
}
