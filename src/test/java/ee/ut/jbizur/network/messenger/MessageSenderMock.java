package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.role.bizur.BizurNodeMock;
import ee.ut.jbizur.role.Role;

import java.util.HashMap;
import java.util.Map;

public class MessageSenderMock implements IMessageSender {
    private final Map<String, Role> roles = new HashMap<>();
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    @Override
    public void send(NetworkCommand command) {
        /* command is marshalled then unmarshalled to prevent receiving process to use the same object
           with the sending process. This can be thought of as a deep-copy of the command object. */
        command = commandMarshaller.unmarshall(commandMarshaller.marshall(command));

        Role receiverRole = roles.get(command.getReceiverAddress().toString());
        if(receiverRole instanceof BizurNodeMock) {
            if(((BizurNodeMock) receiverRole).isDead) {
                Role senderRole = roles.get(command.getSenderAddress().toString());
                senderRole.handleInternalCommand(new SendFail_IC(command));
                return;
            }
        }
        receiverRole.handleNetworkCommand(command);
    }

    public void registerRole(Role role){
        roles.put(role.getConfig().getAddress().toString(), role);
    }
}
