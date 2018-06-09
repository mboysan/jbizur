package network.messenger;

import protocol.CommandMarshaller;
import protocol.commands.NetworkCommand;
import protocol.internal.SendFail_IC;
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

        if(isBroken){
            Role role = roles.get(command.getSenderAddress().toString());
            if(role != null){
                role.handleInternalCommand(new SendFail_IC(command));
            }
        } else {
            Role role = roles.get(command.getReceiverAddress().toString());
            if(role != null) {
                role.handleNetworkCommand(command);
            }
        }
    }

    public boolean isBroken() {
        return isBroken;
    }

    public void setBroken(boolean broken) {
        isBroken = broken;
    }

    public void registerRole(Role role){
        roles.put(role.getAddress().toString(), role);
    }
}
