package ee.ut.jbizur.network.io.tcp.rapidoid;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.AbstractServer;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import org.rapidoid.net.Server;
import org.rapidoid.net.TCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RapidoidServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(RapidoidServer.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private Server rapidoidServer;
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    public RapidoidServer(NetworkManager networkManager) {
        super(networkManager);
        commandMarshaller.setSerializer(new ByteSerializer());
    }

    @Override
    public void startRecv(Address address) {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address is not a TCP address: " + address);
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        rapidoidServer = TCP.server().protocol(ctx -> {
            String line = ctx.readln().trim();
            int length = Integer.parseInt(line);
            byte[] msg = ctx.input().readNbytes(length);
            NetworkCommand command = commandMarshaller.unmarshall(msg);
            executor.submit(() -> {
                networkManager.handleCmd(command);
            });
        }).port(tcpAddress.getPortNumber()).build();
        rapidoidServer.start();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        if (rapidoidServer.isActive()) {
            rapidoidServer.shutdown();
        }
    }
}
