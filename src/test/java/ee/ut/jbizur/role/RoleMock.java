package ee.ut.jbizur.role;

import ee.ut.jbizur.network.messenger.MessageProcessor;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.InternalCommand;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoleMock extends Role {

    public Map<Integer, NetworkCommand> receivedCommandsMap = new ConcurrentHashMap<>();
    private final AtomicInteger recvCmdCount = new AtomicInteger(0);

    public RoleMock(RoleSettings settings) {
        super(settings.setMultiCastEnabled(false));
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        System.out.println("command recv (" + recvCmdCount.incrementAndGet() + "): " + command);
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
