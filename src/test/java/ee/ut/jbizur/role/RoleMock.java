package ee.ut.jbizur.role;

import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.internal.InternalCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class RoleMock extends Role {
    public final AtomicInteger recvCmdCount = new AtomicInteger(0);

    public RoleMock(RoleSettings settings) {
        super(settings.setMultiCastEnabled(false));
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        int cnt = recvCmdCount.incrementAndGet();
        if (cnt % 50 == 0) {
            System.out.println("cnt: " + cnt);
        }
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {

    }

    @Override
    public CompletableFuture start() {
        return null;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }
}
