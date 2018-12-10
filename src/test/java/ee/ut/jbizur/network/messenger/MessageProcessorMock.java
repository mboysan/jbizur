package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.role.Role;

public class MessageProcessorMock extends MessageProcessor {

    public void registerRole(Role role) {
        this.role = role;
    }

    @Override
    protected AbstractClient createClient() {
        return new MessageSenderMock(role);
    }

    @Override
    protected AbstractServer createServer() {
        return new MessageReceiverMock(role);
    }
}
