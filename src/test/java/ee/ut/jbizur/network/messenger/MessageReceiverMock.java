package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageReceiverMock implements IMessageReceiver {
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Role role;

    @Override
    public void startRecv() {
        //nothing needs to be done here
    }

    public void handleNetworkCommand(NetworkCommand command) {
        executor.execute(() -> { role.handleNetworkCommand(command); });
//        role.handleNetworkCommand(command);
    }

    public void registerRole(Role role) {
        this.role = role;
    }

    @Override
    public void shutdown() {

    }
}
