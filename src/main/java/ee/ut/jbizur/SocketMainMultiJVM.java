package ee.ut.jbizur;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.config.UserSettings;
import mpi.MPIException;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ee.ut.jbizur.network.ConnectionProtocol.TCP_CONNECTION;

/**
 * Assumes a single JVM is running per Node.
 */
public class SocketMainMultiJVM {

    public static void main(String[] args) throws UnknownHostException, InterruptedException, MPIException {
        UserSettings settings = new UserSettings(args, TCP_CONNECTION);

        InetAddress ip = TCPAddress.resolveIpAddress();

        MulticastAddress multicastAddress = new MulticastAddress(settings.getGroupName(), settings.getGroupId());
        TCPAddress tcpAddress = new TCPAddress(ip, 0);

        GlobalConfig.getInstance().initTCP(false, multicastAddress);

        BizurNode node = new BizurNode(tcpAddress);

        TimeUnit.SECONDS.sleep(10);

        boolean b = true;
        while (b) {
            String key = UUID.randomUUID().toString();
            node.set(key, UUID.randomUUID().toString());
            node.delete(key);
        }

        node.signalEndToAll();

        GlobalConfig.getInstance().end();
    }
}
