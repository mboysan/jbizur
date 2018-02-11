package network.communication;

import protocol.commands.NetworkCommand;

public interface IMessageHandler {
    void sendCommand(NetworkCommand command);
    void receiveCommand(NetworkCommand command);
}
