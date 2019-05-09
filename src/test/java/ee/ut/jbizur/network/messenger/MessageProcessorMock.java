package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.role.Role;

public class MessageProcessorMock extends MessageProcessor {

    public MessageProcessorMock(Role role) {
        super(role);
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
