package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMock extends AbstractServer {
    private ExecutorService executor = Executors.newCachedThreadPool();

    ServerMock(Role roleInstance) {
        super(roleInstance);
    }

    public void handleNetworkCommand(NetworkCommand command) {
        executor.execute(() -> { roleInstance.handleNetworkCommand(command); });
    }

    @Override
    public void startRecv(Address address) {

    }

    @Override
    public void shutdown() {

    }
}
