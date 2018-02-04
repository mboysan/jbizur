package network.communication;

import protocol.commands.BaseCommand;

public interface IMessageHandler {
    void sendCommand(BaseCommand command);
}
