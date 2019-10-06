package ee.ut.jbizur.network.io.tcp.custom;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.AbstractClient;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.protocol.commands.ic.SendFail_IC;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.SignalEnd_NC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The message sender wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingClientImpl extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(BlockingClientImpl.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Map<String, SendSocket> socketMap = new ConcurrentHashMap<>();

    public BlockingClientImpl(NetworkManager networkManager) {
        super(networkManager);
        if (!super.keepAlive) {
            throw new UnsupportedOperationException("non keepalive connection type not supported");
        }
    }

    /**
     * {@inheritDoc}
     * Initializes the message sender. It then creates the appropriate handler to _send the message.
     */
    @Override
    public void send(NetworkCommand message) {
        try {
            TCPAddress receiverAddress = (TCPAddress) message.getReceiverAddress();
            SendSocket senderSocket = connect(receiverAddress);
            if (message instanceof SignalEnd_NC) {
                try {
                    senderSocket.send(message);
                } catch (IOException e) {
                    logger.error("Send err, msg (1): {}", message, e);
                    networkManager.handleCmd(new SendFail_IC(message));
                }
                shutdown();
            } else {
                executor.submit(() -> {
                    try {
                        senderSocket.send(message);
                    } catch (IOException e) {
                        logger.error("Send err, msg (2): {}", message, e);
                        networkManager.handleCmd(new SendFail_IC(message));
                    }
                });
            }
        } catch (IOException e) {
            logger.error("Client could not connect to server.", e);
            networkManager.handleCmd(new SendFail_IC(message));
        }
    }

    @Override
    protected <T> T connect(Address address) throws IOException {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address must be a TCP address");
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        SendSocket senderSocket = socketMap.get(tcpAddress.toString());
        if (senderSocket == null) {
            senderSocket = createSenderSocket(tcpAddress);
            socketMap.put(tcpAddress.toString(), senderSocket);
        }
        return (T) senderSocket;
    }

    protected SendSocket createSenderSocket(TCPAddress tcpAddress) throws IOException {
        return new SendSocket(
                new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()),
                true
        );
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
        disconnectAll();
        logger.info("Client shutdown: {}", networkManager.toString());
    }

    protected void disconnect(String tcpAddressStr, SendSocket senderSocket) throws IOException {
        if (senderSocket != null) {
            senderSocket.close();
            socketMap.remove(tcpAddressStr);
        }
    }

    protected void disconnectAll() {
        socketMap.forEach((tcpAddressStr, socket) -> {
            try {
                disconnect(tcpAddressStr, socket);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        });
        socketMap.clear();
    }
}
