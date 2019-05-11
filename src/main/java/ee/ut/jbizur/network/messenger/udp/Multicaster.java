package ee.ut.jbizur.network.messenger.udp;

import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.util.ByteUtils;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Used for sending multicast messages. Preferably used for process discovery with
 * {@link ee.ut.jbizur.network.ConnectionProtocol#TCP}
 */
public class Multicaster {

    private volatile boolean isRunning = true;

    private final MulticastAddress multicastAddress;

    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    /**
     * For handling received multicast messages.
     */
    private final Role roleInstance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MulticastReceiver receiver;

    public Multicaster(MulticastAddress multicastAddress, Role roleInstance) {
        if(multicastAddress == null){
            throw new IllegalArgumentException("multicast address has to be provided");
        }
        this.multicastAddress = multicastAddress;
        this.roleInstance = roleInstance;
        this.receiver = new MulticastReceiver();
    }

    public void multicast(NetworkCommand messageToSend) {
        new MulticastPublisher().multicast(messageToSend);
    }

    public void startRecv() {
        executor.execute(receiver);
    }

    /**
     * shutdown the multicaster service.
     */
    public void shutdown(){
        isRunning = false;
        executor.shutdown();
        receiver.end();
    }

    private class MulticastPublisher {
        public void multicast(NetworkCommand messageToSend) {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress group = multicastAddress.getMulticastGroupAddr();
                byte[] msg = commandMarshaller.marshall(messageToSend, byte[].class);
                byte[] msgWithLength = ByteUtils.prependMessageLengthTo(msg);

                DatagramPacket packet
                        = new DatagramPacket(msgWithLength, msgWithLength.length, group, multicastAddress.getMulticastPort());
                socket.send(packet);
            } catch (IOException e) {
                Logger.error(e, "multicast _send error.");
            }
        }
    }

    private class MulticastReceiver implements Runnable {
        private ExecutorService executor = Executors.newSingleThreadExecutor();
        private MulticastSocket socket;

        @Override
        public void run() {
            recv();
        }

        private void end(){
            isRunning = false;
            executor.shutdown();
            shutdown();
            new MulticastPublisher().multicast(new SignalEnd_NC());
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
                        Logger.info("MulticastReceiver end!");
                        break;
                    }
                    executor.execute(() -> roleInstance.handleNetworkCommand(received));
                }
                shutdown();
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    Logger.warn("Socket might be closed: " + e);
                } else {
                    Logger.error(e, "multicast recv error");
                }
            }
        }

        private void shutdown() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.leaveGroup(multicastAddress.getMulticastGroupAddr());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.close();
            }
        }
    }

}
