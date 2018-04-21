package network.messenger;

import protocol.CommandMarshaller;
import protocol.commands.NetworkCommand;
import protocol.commands.bizur.ReplicaWrite_NC;
import role.Role;

import java.util.HashMap;
import java.util.Map;

public class MessageSenderMock implements IMessageSender {

    public Map<String, Role> rolesMap = new HashMap<>();
    CommandMarshaller commandMarshaller = new CommandMarshaller();

    @Override
    public void send(NetworkCommand command) {
        /* command is marshalled then unmarshalled to prevent receiving process to use the same object
           with the sending process. This can be thought of as a deep-copy of the command object. */
        if(command instanceof ReplicaWrite_NC){
//            System.out.println();
        }
        command = commandMarshaller.unmarshall(commandMarshaller.marshall(command));

        Role role = rolesMap.get(command.getReceiverAddress().toString());
        if(role != null){
            role.handleMessage(command);
        }
    }

    public void registerRole(String address, Role role){
        rolesMap.put(address, role);
    }
}
