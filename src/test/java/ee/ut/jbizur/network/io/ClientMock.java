package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.ic.SendFail_IC;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.role.bizur.BizurClientMock;
import ee.ut.jbizur.role.bizur.BizurNodeMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static ee.ut.jbizur.network.io.NetworkManagerMock.getRole;

public class ClientMock extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(ClientMock.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Future<?>> tasks = new ArrayList<>();

    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    ClientMock(NetworkManagerMock networkManagerMock) {
        super(networkManagerMock);
    }

    @Override
    public void send(NetworkCommand command) {
        /* command is marshalled then unmarshalled to prevent receiving process to use the same object
           with the sending process. This can be thought of as a deep-copy of the command object. */
        command = commandMarshaller.unmarshall(commandMarshaller.marshall(command));

        NetworkCommand finalCommand = command;
        Runnable r = () -> {
            Role receiverRole = getRole(finalCommand.getReceiverAddress());
            if (receiverRole instanceof BizurNodeMock) {
                if(((BizurNodeMock) receiverRole).isDead) {
                    Role senderRole = getRole(finalCommand.getSenderAddress());
                    if (senderRole instanceof BizurNodeMock) {
                        ((BizurNodeMock) senderRole).getNetworkManager().getServer().recv(new SendFail_IC(finalCommand));
                    } else if (senderRole instanceof BizurClientMock) {
                        ((BizurClientMock) senderRole).getNetworkManager().getServer().recv(new SendFail_IC(finalCommand));
                    } else {
                        throw new UnsupportedOperationException("role cannot handle (1): " + senderRole);
                    }
                } else {
                    ((BizurNodeMock) receiverRole).getNetworkManager().getServer().recv(finalCommand);
                }
            } else if (receiverRole instanceof BizurClientMock) {
                ((BizurClientMock) receiverRole).getNetworkManager().getServer().recv(finalCommand);
            } else {
                throw new UnsupportedOperationException("role cannot handle (2): " + receiverRole);
            }
        };
        if (Conf.get().tests.functional.clientMultiThreading) {
            tasks.add(executor.submit(r));
        } else {
            r.run();
        }
    }

    @Override
    protected <T> T connect(Address address) throws IOException {
        return null;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
