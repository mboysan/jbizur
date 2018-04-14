import config.GlobalConfig;
import config.UserSettings;
import mpi.MPIException;
import network.address.MulticastAddress;
import network.address.TCPAddress;
import org.pmw.tinylog.Logger;
import role.Node;
import testframework.SystemMonitor;
import testframework.TestFramework;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static network.ConnectionProtocol.TCP_CONNECTION;

/**
 * Assumes a single JVM is running per Node.
 */
public class SocketMainMultiJVM {

    public static void main(String[] args) throws UnknownHostException, InterruptedException, MPIException {
        long timeStart = System.currentTimeMillis();

        UserSettings settings = new UserSettings(args, TCP_CONNECTION);

        SystemMonitor sysInfo = null;
        TestFramework testFramework = null;

        if(settings.isMonitorSystem()){
            sysInfo = SystemMonitor.collectEvery(500, TimeUnit.MILLISECONDS);
        }

        InetAddress ip = TCPAddress.resolveIpAddress();

        MulticastAddress multicastAddress = new MulticastAddress(settings.getGroupName(), settings.getGroupId());
        TCPAddress tcpAddress = new TCPAddress(ip, 0);

        GlobalConfig.getInstance().initTCP(false, multicastAddress);

        Node node = new Node(tcpAddress);

        if(node.isLeader()){    // the node is pinger.
            /* start tests */
            testFramework = TestFramework.doPingTests(node, settings.getTaskCount());

            /* send end signal to all nodes */
            node.signalEndToAll();
        }

        GlobalConfig.getInstance().end();

        TimeUnit.MILLISECONDS.sleep(500);

        if(testFramework != null){
            testFramework.printAllOnConsole();
        }

        if(sysInfo != null){
            sysInfo.printOnConsole();
        }

        Logger.info("Total time (ms): " + (System.currentTimeMillis() - timeStart));
    }
}
