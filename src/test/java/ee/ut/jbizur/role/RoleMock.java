package ee.ut.jbizur.role;

import ee.ut.jbizur.network.messenger.MessageProcessor;
import ee.ut.jbizur.network.messenger.udp.Multicaster;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.InternalCommand;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RoleMock extends Role {

    public Map<Integer, NetworkCommand> receivedCommandsMap = new ConcurrentHashMap<>();

    public RoleMock(RoleSettings settings) throws InterruptedException, UnknownHostException {
        super(settings, new MessageProcessor() {
            @Override
            protected Multicaster createMulticaster() {
                return null;
            }
            @Override
            protected void initMulticast() {

            }
        });
        messageProcessor.registerRole(this);
        messageProcessor.start();
    }

    @Override
    protected void initRole() {
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        System.out.println("command recv: " + command);
        receivedCommandsMap.put(command.getMsgId(), command);
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {

    }

    @Override
    public CompletableFuture start() {
        return null;
    }

    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }
}
