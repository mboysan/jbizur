package ee.ut.jbizur.role;

import ee.ut.jbizur.network.messenger.NetworkManager;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.InternalCommand;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoleMock extends Role {
    public final AtomicInteger recvCmdCount = new AtomicInteger(0);

    public RoleMock(RoleSettings settings) {
        super(settings.setMultiCastEnabled(false));
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        recvCmdCount.incrementAndGet();
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {

    }

    @Override
    public CompletableFuture start() {
        return null;
    }

    public NetworkManager getMessageProcessor() {
        return networkManager;
    }
}
