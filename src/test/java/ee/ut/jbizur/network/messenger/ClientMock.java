package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.role.bizur.BizurClientMock;
import ee.ut.jbizur.role.bizur.BizurNodeMock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientMock extends AbstractClient {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, Role> roles = new HashMap<>();
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    ClientMock(Role roleInstance) {
        super(roleInstance);
    }

    @Override
    public void send(NetworkCommand command) {
        /* command is marshalled then unmarshalled to prevent receiving process to use the same object
           with the sending process. This can be thought of as a deep-copy of the command object. */
        command = commandMarshaller.unmarshall(commandMarshaller.marshall(command));
        executor.execute(new Sender(command));
    }

    public void registerRole(Role role){
        roles.put(role.getSettings().getAddress().toString(), role);
    }

    @Override
    protected <T> T connect(Address address) throws IOException {
        return null;
    }

    private class Sender implements Runnable {
        private final NetworkCommand command;

        public Sender(NetworkCommand command) {
            this.command = command;
        }

        @Override
        public void run() {
            Role receiverRole = roles.get(command.getReceiverAddress().toString());
            if(receiverRole instanceof BizurNodeMock) {
                if(((BizurNodeMock) receiverRole).isDead) {
                    Role senderRole = roles.get(command.getSenderAddress().toString());
                    senderRole.handleInternalCommand(new SendFail_IC(command));
                    return;
                }
            }
            if (receiverRole instanceof BizurNodeMock) {
                ((BizurNodeMock) receiverRole).sendCommandToMessageReceiver(command);
            } else if (receiverRole instanceof BizurClientMock) {
                ((BizurClientMock) receiverRole).sendCommandToMessageReceiver(command);
            } else {
//                receiverRole.handleNetworkCommand(command);
                throw new UnsupportedOperationException("role cannot handle it now: " + receiverRole.toString());
            }
        }
    }
}
