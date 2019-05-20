package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractClient;
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

    private final ExecutorService executor;
    private Map<String, SendSocket> socketMap;

    public BlockingClientImpl(Role roleInstance) {
        super(roleInstance);
        this.executor = Executors.newCachedThreadPool();
        if (keepAlive) {
            socketMap = new ConcurrentHashMap<>();
        }
    }

    /**
     * {@inheritDoc}
     * Initializes the message sender. It then creates the appropriate handler to _send the message.
     */
    @Override
    public void send(NetworkCommand message) {
        Runnable sender = createSender(message);
        if(message instanceof SignalEnd_NC){
            sender.run();
            shutdown();
            Logger.info("Client executor shutdown [" + executor.isShutdown() + "], info=[" + executor + "]");
        } else {
            executor.execute(sender);
        }
    }


    private Runnable createSender(final NetworkCommand message) {
        return () -> {
            SendSocket senderSocket = null;
            TCPAddress receiverAddress = null;
            try {
                receiverAddress = (TCPAddress) message.getReceiverAddress();
                senderSocket = connect(receiverAddress);
                senderSocket.send(message);
            } catch (IOException e) {
                Logger.error("Send err, msg: " + message + ", " + e, e);
                roleInstance.handleInternalCommand(new SendFail_IC(message));
            } finally {
                if (!keepAlive) {
                    try {
                        if (receiverAddress != null) {
                            disconnect(receiverAddress.toString(), senderSocket);
                        }
                    } catch (IOException e) {
                        Logger.error(e);
                    }
                }
            }
        };
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
    }

    @Override
    protected <T> T connect(Address address) throws IOException {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address must be a TCP address");
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        SendSocket senderSocket;
        if (keepAlive) {
            senderSocket = socketMap.get(tcpAddress.toString());
            if (senderSocket == null) {
                senderSocket = createSenderSocket(tcpAddress);
                socketMap.put(tcpAddress.toString(), senderSocket);
            }
        } else {
            senderSocket = createSenderSocket(tcpAddress);
        }
        return (T) senderSocket;
    }

    protected SendSocket createSenderSocket(TCPAddress tcpAddress) throws IOException {
        return new SendSocket(
                new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()),
                keepAlive
        );
    }

    protected void disconnect(String tcpAddressStr, SendSocket senderSocket) throws IOException {
        if (senderSocket != null) {
            senderSocket.close();
            if (keepAlive) {
                socketMap.remove(tcpAddressStr);
            }
        }
    }

    protected void disconnectAll() {
        if (socketMap == null) {
            return;
        }
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
