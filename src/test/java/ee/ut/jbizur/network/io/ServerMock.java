package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.ICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ServerMock extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(ServerMock.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Future<?>> tasks = new ArrayList<>();

    ServerMock(NetworkManagerMock networkManagerMock) {
        super(networkManagerMock);
    }

    @Override
    public void startRecv(Address address) {

    }

    public void recv(ICommand command) {
        if (Conf.get().tests.functional.serverMultiThreading) {
            tasks.add(executor.submit(() -> networkManager.handleCmd(command)));
        } else {
            networkManager.handleCmd(command);
        }
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
