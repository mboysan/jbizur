package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.messenger.MessageProcessor;
import ee.ut.jbizur.network.messenger.MessageReceiverMock;
import ee.ut.jbizur.network.messenger.MessageSenderMock;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

public class BizurClientMock extends BizurClient {

    public BizurClientMock(BizurSettings bizurSettings, MessageProcessor messageProcessor) throws InterruptedException {
        super(bizurSettings, messageProcessor);
    }

    @Override
    public void initRole() {
        super.initRole();
    }

    public void registerRoles(Role[] roles){
        for (Role role : roles) {
            registerRole(role);
        }
    }

    public void registerRole(Role role) {
        ((MessageSenderMock) messageProcessor.getClient()).registerRole(role);
    }

    public void sendCommandToMessageReceiver(NetworkCommand command) {
        ((MessageReceiverMock) messageProcessor.getServer()).handleNetworkCommand(command);
    }
}
