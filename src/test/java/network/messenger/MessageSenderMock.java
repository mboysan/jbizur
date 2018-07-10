package network.messenger;

import protocol.CommandMarshaller;
import protocol.commands.NetworkCommand;
import protocol.internal.SendFail_IC;
import role.BizurNodeMock;
import role.Role;

import java.util.HashMap;
import java.util.Map;

public class MessageSenderMock implements IMessageSender {
    private boolean isBroken = false;

    private final Map<String, Role> roles = new HashMap<>();
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    @Override
    public void send(NetworkCommand command) {
        /* command is marshalled then unmarshalled to prevent receiving process to use the same object
           with the sending process. This can be thought of as a deep-copy of the command object. */
        command = commandMarshaller.unmarshall(commandMarshaller.marshall(command));

        Role receiverRole = roles.get(command.getReceiverAddress().toString());
        if(receiverRole instanceof BizurNodeMock) {
            if(((BizurNodeMock) receiverRole).isDead()) {
                Role senderRole = roles.get(command.getSenderAddress().toString());
                senderRole.handleInternalCommand(new SendFail_IC(command));
                return;
            }
        }
        receiverRole.handleNetworkCommand(command);
    }

    public void registerRole(Role role){
        roles.put(role.getAddress().toString(), role);
    }
}
