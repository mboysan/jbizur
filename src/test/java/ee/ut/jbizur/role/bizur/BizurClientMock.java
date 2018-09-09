package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.network.messenger.MessageSenderMock;
import ee.ut.jbizur.network.messenger.Multicaster;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.CountDownLatch;

public class BizurClientMock extends BizurClient {

    public BizurClientMock(BizurSettings bizurSettings, Multicaster multicaster, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        super(bizurSettings, multicaster, messageSender, messageReceiver, readyLatch);
    }

    public void registerRoles(Role[] roles){
        for (Role role : roles) {
            registerRole(role);
        }
    }

    public void registerRole(Role role) {
        ((MessageSenderMock) messageSender).registerRole(role);
    }
}
