package ee.ut.jbizur.network.io.udp;

import ee.ut.jbizur.common.config.Conf;
import ee.ut.jbizur.common.protocol.CommandMarshaller;
import ee.ut.jbizur.common.protocol.address.MulticastAddress;
import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.common.protocol.commands.nc.ping.SignalEnd_NC;
import ee.ut.jbizur.network.io.AbstractServer;
import ee.ut.jbizur.network.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Multicaster extends AbstractServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Multicaster.class);

    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    /**
     * 2 threads for handling send/recv concurrently. These are low priority operations.
     */
    private final ScheduledExecutorService schExecutor = Executors.newScheduledThreadPool(2);

    private final MulticastReceiver receiver;

    public Multicaster(String name, MulticastAddress multicastAddress) {
        super(name, multicastAddress);
        this.receiver = new MulticastReceiver();
    }

    public void start() {
        super.start();
        submit(receiver);
    }

    public void multicastAtFixedRate(NetworkCommand command) {
        validateAction();
        schExecutor.scheduleAtFixedRate(() -> {
            multicast(command);
        }, 0, Conf.get().network.multicast.intervalms, TimeUnit.MILLISECONDS);
    }

    public void multicast(NetworkCommand command) {
        validateAction();
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = getServerAddress().getMulticastGroupAddr();
            byte[] msg = commandMarshaller.marshall(command, byte[].class);
            byte[] msgWithLength = ByteUtil.prependMessageLengthTo(msg);

            DatagramPacket packet
                    = new DatagramPacket(msgWithLength, msgWithLength.length, group, getServerAddress().getMulticastPort());
            socket.send(packet);
        } catch (IOException e) {
            logger.error("multicast _send error.", e);
        }
    }

    /**
     * shutdown the multicaster service.
     */
    @Override
    public void close() {
        try {
            receiver.close();
            schExecutor.shutdown();
            schExecutor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            super.close();
        }
    }

    private class MulticastReceiver implements Runnable, AutoCloseable {
        private MulticastSocket socket;

        @Override
        public void run() {
            try {
                socket = new MulticastSocket(getServerAddress().getMulticastPort());
                InetAddress group = getServerAddress().getMulticastGroupAddr();
                socket.joinGroup(group);
                byte[] msg = new byte[1024];    //fixed size byte[]
                while (isRunning()) {
                    DatagramPacket packet = new DatagramPacket(msg, msg.length);
                    synchronized (socket) {
                        socket.receive(packet);
                    }
                    byte[] msgRecv = ByteUtil.extractActualMessage(packet.getData());

                    NetworkCommand received = commandMarshaller.unmarshall(msgRecv);
                    if (received instanceof SignalEnd_NC) {
                        logger.info("MulticastReceiver end!");
                        break;
                    }
                    recvAsync(received);
                }
                close();
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    logger.warn("Socket might be closed", e);
                } else {
                    logger.error("multicast recv error", e);
                }
            }
        }

        @Override
        public void close() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.leaveGroup(getServerAddress().getMulticastGroupAddr());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                socket.close();
            }
        }
    }

    @Override
    public MulticastAddress getServerAddress() {
        return (MulticastAddress) super.getServerAddress();
    }
}
