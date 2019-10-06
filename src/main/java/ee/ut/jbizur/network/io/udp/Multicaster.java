package ee.ut.jbizur.network.io.udp;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.Connect_NC;
import ee.ut.jbizur.protocol.commands.nc.ping.SignalEnd_NC;
import ee.ut.jbizur.role.RoleSettings;
import ee.ut.jbizur.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Used for sending multicast messages. Preferably used for process discovery with
 * {@link ee.ut.jbizur.network.ConnectionProtocol#TCP}
 */
public class Multicaster {

    private static final Logger logger = LoggerFactory.getLogger(Multicaster.class);

    private volatile boolean isRunning = true;

    private final RoleSettings roleSettings;
    private final MulticastAddress multicastAddress;

    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    /**
     * For handling received multicast messages.
     */
    private final NetworkManager networkManager;

    /**
     * 2 threads for handling send/recv concurrently. These are low priority operations.
     */
    private final ScheduledExecutorService schExecutor = Executors.newScheduledThreadPool(2);

    private final MulticastReceiver receiver;

    public Multicaster(NetworkManager networkManager, RoleSettings roleSettings) {
        if (roleSettings == null || roleSettings.getMulticastAddress() == null) {
            throw new IllegalArgumentException("role settings and multicast address have to be provided");
        }
        this.networkManager = networkManager;
        this.roleSettings = roleSettings;
        this.multicastAddress = roleSettings.getMulticastAddress();

        this.receiver = new MulticastReceiver();
    }

    public void initMulticast() {
        startRecv();
        schExecutor.scheduleAtFixedRate(() -> {
            multicast(
                    new Connect_NC()
                            .setSenderId(roleSettings.getRoleId())
                            .setSenderAddress(roleSettings.getAddress())
                            .setNodeType("member")
            );
        }, 0, Conf.get().network.multicast.intervalms, TimeUnit.MILLISECONDS);
    }

    public void startRecv() {
        schExecutor.execute(receiver);
    }

    /**
     * shutdown the multicaster service.
     */
    public void shutdown() {
        isRunning = false;
        receiver.shutdown();
        schExecutor.shutdown();
        try {
            schExecutor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        schExecutor.shutdown();
        try {
            schExecutor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    public void multicast(NetworkCommand messageToSend) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = multicastAddress.getMulticastGroupAddr();
            byte[] msg = commandMarshaller.marshall(messageToSend, byte[].class);
            byte[] msgWithLength = ByteUtils.prependMessageLengthTo(msg);

            DatagramPacket packet
                    = new DatagramPacket(msgWithLength, msgWithLength.length, group, multicastAddress.getMulticastPort());
            socket.send(packet);
        } catch (IOException e) {
            logger.error("multicast _send error.", e);
        }
    }

    private class MulticastReceiver implements Runnable {
        private MulticastSocket socket;

        @Override
        public void run() {
            recv();
        }

        private void recv() {
            try {
                socket = new MulticastSocket(multicastAddress.getMulticastPort());
                InetAddress group = multicastAddress.getMulticastGroupAddr();
                socket.joinGroup(group);
                byte[] msg = new byte[1024];    //fixed size byte[]
                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(msg, msg.length);
                    synchronized (socket) {
                        socket.receive(packet);
                    }
                    byte[] msgRecv = ByteUtils.extractActualMessage(packet.getData());

                    NetworkCommand received = commandMarshaller.unmarshall(msgRecv);
                    if (received instanceof SignalEnd_NC) {
                        logger.info("MulticastReceiver end!");
                        break;
                    }
                    schExecutor.execute(() -> networkManager.handleCmd(received));
                }
                shutdown();
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    logger.warn("Socket might be closed", e);
                } else {
                    logger.error("multicast recv error", e);
                }
            }
        }

        private void shutdown() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.leaveGroup(multicastAddress.getMulticastGroupAddr());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                socket.close();
            }
        }
    }

}
