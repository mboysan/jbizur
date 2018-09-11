package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.messenger.*;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.CountDownLatch;

public class BizurNodeMock extends BizurNode {

    public boolean isDead = false;

    protected BizurNodeMock(BizurSettings bizurSettings, MulticasterMock multicasterMock, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        super(bizurSettings, multicasterMock, messageSender, messageReceiver, readyLatch);
    }

    public void kill() {
        isDead = true;
    }

    public void revive() {
        isDead = false;
    }

    public void registerRoles(Role[] roles){
        for (Role role : roles) {
            registerRole(role);
        }
    }

    public void registerRole(Role role) {
        ((MessageSenderMock) messageSender).registerRole(role);
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        if(isDead) {
            super.handleNetworkCommand(new Nack_NC());
        } else {
            super.handleNetworkCommand(command);
        }
    }

    public void sendCommandToMessageReceiver(NetworkCommand command) {
        if (messageReceiver instanceof MessageReceiverMock) {
            ((MessageReceiverMock) messageReceiver).handleNetworkCommand(command);
        }
    }
}
