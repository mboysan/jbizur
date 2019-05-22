package ee.ut.jbizur.network.io.tcp.custom;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.AbstractClient;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

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

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Map<String, SendSocket> socketMap = new ConcurrentHashMap<>();

    public BlockingClientImpl(Role roleInstance) {
        super(roleInstance);
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
                    Logger.error("Send err, msg (1): " + message + ", " + e, e);
                    roleInstance.handleInternalCommand(new SendFail_IC(message));
                }
                shutdown();
            } else {
                executor.submit(() -> {
                    try {
                        senderSocket.send(message);
                    } catch (IOException e) {
                        Logger.error("Send err, msg (2): " + message + ", " + e, e);
                        roleInstance.handleInternalCommand(new SendFail_IC(message));
                    }
                });
            }
        } catch (IOException e) {
            Logger.error("Client could not connect to server.", e);
            roleInstance.handleInternalCommand(new SendFail_IC(message));
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
        super.shutdown();
        executor.shutdown();
        try {
            executor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.error(e);
        }
        disconnectAll();
        Logger.info("Client shutdown: " + roleInstance.toString());
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
                Logger.error(e);
            }
        });
        socketMap.clear();
    }
}
