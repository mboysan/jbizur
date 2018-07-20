package role;

import network.address.Address;
import network.messenger.IMessageReceiver;
import network.messenger.IMessageSender;
import protocol.commands.NetworkCommand;
import protocol.commands.common.Nack_NC;

import java.util.concurrent.CountDownLatch;

public class BizurNodeMock extends BizurNode {

    public boolean isDead = false;

    public BizurNodeMock(Address baseAddress) throws InterruptedException {
        super(baseAddress);
    }

    protected BizurNodeMock(Address baseAddress, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        super(baseAddress, messageSender, messageReceiver, readyLatch);
    }

    public void kill() {
        isDead = true;
    }

    public void revive() {
        isDead = false;
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        if(isDead) {
            super.handleNetworkCommand(new Nack_NC());
        } else {
            super.handleNetworkCommand(command);
        }
    }
}
