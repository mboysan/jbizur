package ee.ut.jbizur;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.config.UserSettings;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurNode;
import mpi.MPIException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ee.ut.jbizur.network.ConnectionProtocol.TCP_CONNECTION;

/**
 * Assumes a single JVM is running per Node.
 */
public class InitMainMultiJVM {

    public static void main(String[] args) throws UnknownHostException, InterruptedException, MPIException {
        UserSettings settings = new UserSettings(args, TCP_CONNECTION);

        MulticastAddress multicastAddress = new MulticastAddress("all-systems.mcast.net", 9090);
        if (settings.getGroupName() != null && settings.getGroupId() >= 0) {
            multicastAddress = new MulticastAddress(settings.getGroupName(), settings.getGroupId());
        }

        InetAddress ip = TCPAddress.resolveIpAddress();
        TCPAddress tcpAddress = new TCPAddress(ip, 0);

        GlobalConfig.getInstance().initTCP(multicastAddress);

        BizurNode node = new BizurNode(tcpAddress);
    }
}
