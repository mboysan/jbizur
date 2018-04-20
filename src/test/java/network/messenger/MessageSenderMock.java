package network.messenger;

import config.GlobalConfig;
import network.messenger.IMessageSender;
import protocol.commands.NetworkCommand;
import role.Role;

import java.util.HashMap;
import java.util.Map;

public class MessageSenderMock implements IMessageSender {

    public Map<String, Role> rolesMap = new HashMap<>();

    @Override
    public void send(NetworkCommand command) {
        Role role = rolesMap.get(command.resolveReceiverAddress().toString());
        if(role != null){
            role.handleMessage(command);
        }
    }

    public void registerRole(String address, Role role){
        rolesMap.put(address, role);
    }
}
