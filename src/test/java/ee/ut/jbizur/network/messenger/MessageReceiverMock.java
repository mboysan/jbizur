package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageReceiverMock extends AbstractServer {
    private ExecutorService executor = Executors.newCachedThreadPool();

    public MessageReceiverMock(Role roleInstance) {
        super(roleInstance);
    }

    public void handleNetworkCommand(NetworkCommand command) {
        executor.execute(() -> { roleInstance.handleNetworkCommand(command); });
//        role.handleNetworkCommand(command);
    }

    public void registerRole(Role role) {
        this.roleInstance = role;
    }

    @Override
    public void startRecv(Address address) {

    }

    @Override
    public void shutdown() {

    }
}
