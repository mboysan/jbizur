package ee.ut.jbizur.role;

import ee.ut.jbizur.network.address.MockMulticastAddress;
import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.InternalCommand;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RoleMock extends Role {

    public Map<String, NetworkCommand> receivedCommandsMap = new ConcurrentHashMap<>();

    public RoleMock(RoleSettings settings) throws InterruptedException, UnknownHostException {
        super(
                new RoleSettings()
                        .setMulticastAddress(new MockMulticastAddress((String) null, 0))
        );
    }

    @Override
    protected void initMulticast() {

    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        receivedCommandsMap.put(command.getMsgId(), command);
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {

    }

    @Override
    public CompletableFuture start() {
        return null;
    }

    public IMessageSender getMessageSender() {
        return messageSender;
    }

    public IMessageReceiver getMessageReceiver() {
        return messageReceiver;
    }
}
