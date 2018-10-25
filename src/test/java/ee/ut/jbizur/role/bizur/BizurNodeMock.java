package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.BizurTestConfig;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.network.messenger.MessageProcessor;
import ee.ut.jbizur.network.messenger.MessageReceiverMock;
import ee.ut.jbizur.network.messenger.MessageSenderMock;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.atomic.AtomicInteger;

public class BizurNodeMock extends BizurNode {

    public boolean isDead = false;
    public AtomicInteger hashIndex = new AtomicInteger(-1);

    protected BizurNodeMock(BizurSettings bizurSettings, MessageProcessor messageProcessor) throws InterruptedException {
        super(bizurSettings, messageProcessor);
    }

    @Override
    public void initRole() {
        super.initRole();
    }

    @Override
    protected BucketContainer createBucketContainer() {
        return new BucketContainer(BizurTestConfig.getBucketCount()) {
            @Override
            public int hashKey(String s) {
                int idx = hashIndex.get();
                return idx < 0
                        ? super.hashKey(s)
                        : idx;
            }
        }.initBuckets();
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
        ((MessageSenderMock) messageProcessor.getClient()).registerRole(role);
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
        ((MessageReceiverMock) messageProcessor.getServer()).handleNetworkCommand(command);
    }
}
