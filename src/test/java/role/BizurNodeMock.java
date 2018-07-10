package role;

import network.address.Address;
import network.messenger.IMessageReceiver;
import network.messenger.IMessageSender;
import protocol.commands.NetworkCommand;
import protocol.commands.common.Nack_NC;

import java.util.concurrent.CountDownLatch;

public class BizurNodeMock extends BizurNode {

    private boolean isDead = false;
    private boolean handlerTimeout = false;
    private boolean messageSenderBroke = false;

    public BizurNodeMock(Address baseAddress) throws InterruptedException {
        super(baseAddress);
    }

    protected BizurNodeMock(Address baseAddress, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        super(baseAddress, messageSender, messageReceiver, readyLatch);
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public boolean isHandlerTimeout() {
        return handlerTimeout;
    }

    public void setHandlerTimeout(boolean handlerTimeout) {
        this.handlerTimeout = handlerTimeout;
    }

    public IMessageSender getMessageSender(){
        return messageSender;
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        if(isDead) {
            super.handleNetworkCommand(new Nack_NC());
        } else if(isHandlerTimeout()){
            //do nothing
        } else {
            super.handleNetworkCommand(command);
        }
    }
}
