package network.client;

import network.communication.IMessageHandler;

public class ClientMessageHandler implements IMessageHandler {

    private final IClient client;

    public ClientMessageHandler(IClient client) {
        this.client = client;
    }

    @Override
    public void sendCommand(Object command) {

    }

}
