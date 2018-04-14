package network.messenger;

import network.address.MulticastAddress;
import org.pmw.tinylog.Logger;
import protocol.CommandMarshaller;
import protocol.commands.NetworkCommand;
import protocol.commands.ping.SignalEnd_NC;
import role.Role;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Used for sending multicast messages. Preferably used for process discovery with
 * {@link network.ConnectionProtocol#TCP_CONNECTION}
 */
public class Multicaster {

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
        executor.execute(receiver);
    }

    public void multicast(NetworkCommand messageToSend) {
        new MulticastPublisher().multicast(messageToSend);
    }

    /**
     * shutdown the multicaster service.
     */
    public void shutdown(){
        executor.shutdown();
        receiver.end();
    }

    private class MulticastPublisher {
        public void multicast(NetworkCommand messageToSend) {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress group = multicastAddress.getMulticastGroupAddr();
                byte[] msg = commandMarshaller.marshall(messageToSend, byte[].class);

                DatagramPacket packet
                        = new DatagramPacket(msg, msg.length, group, multicastAddress.getMulticastPort());
                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                Logger.error(e, "multicast send error.");
            }
        }
    }

    private class MulticastReceiver implements Runnable {
        private ExecutorService executor = Executors.newSingleThreadExecutor();

        @Override
        public void run() {
            recv();
        }

        private void end(){
            new MulticastPublisher().multicast(new SignalEnd_NC());
        }

        private void recv() {
            try {
                MulticastSocket socket = new MulticastSocket(multicastAddress.getMulticastPort());
                InetAddress group = multicastAddress.getMulticastGroupAddr();
                socket.joinGroup(group);
                byte[] msg = new byte[512];    //fixed size byte[]
                while (true) {
                    DatagramPacket packet = new DatagramPacket(msg, msg.length);
                    socket.receive(packet);
                    NetworkCommand received = commandMarshaller.unmarshall(
                            new String(packet.getData(), StandardCharsets.UTF_8));
                    if(received instanceof SignalEnd_NC){
                        Logger.debug("End signal recv (multicast): " + received);
                        /* No need to handle the end message. */
                        executor.shutdown();
                        break;
                    }
                    executor.execute(() -> roleInstance.handleMessage(received));
                }
                socket.leaveGroup(group);
                socket.close();
            } catch (IOException e) {
                Logger.error(e, "multicast recv error");
            }
        }
    }

}
