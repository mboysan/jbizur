import config.GlobalConfig;
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

/**
 * Assumes a single JVM is running per Node.
 */
public class SocketMainMultiJVM {

    public static void main(String[] args) throws UnknownHostException, InterruptedException, MPIException {
        long timeStart = System.currentTimeMillis();

        Logger.info("Args received: " + Arrays.toString(args));
        int nTasks = 3;     //Number of tasks to complete. Usually equals to (numNodes * numTasksPerNode)
        int multicastPort = 9090;
        String multicastGroup = "all-systems.mcast.net";
        boolean monitorSystem = true;
        if(args != null){
            if(args.length >= 1){
                nTasks = Integer.parseInt(args[0]);
            }
            if(args.length >= 2){
                monitorSystem = Boolean.valueOf(args[1]);
            }
            if(args.length >= 3){
                multicastPort = Integer.parseInt(args[2]);
            }
            if(args.length >= 4){
                multicastGroup = args[3];
            }
        }

        SystemMonitor sysInfo = null;
        TestFramework testFramework = null;

        if(monitorSystem){
            sysInfo = SystemMonitor.collectEvery(500, TimeUnit.MILLISECONDS);
        }

        InetAddress ip = TCPAddress.resolveIpAddress();

        MulticastAddress multicastAddress = new MulticastAddress(multicastGroup, multicastPort);
        TCPAddress tcpAddress = new TCPAddress(ip, 0);

        GlobalConfig.getInstance().initTCP(false, multicastAddress);

        Node node = new Node(tcpAddress);

        if(node.isLeader()){    // the node is pinger.
            /* start tests */
            testFramework = TestFramework.doPingTests(node, nTasks);

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
