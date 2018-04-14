import config.GlobalConfig;
import mpi.MPIException;
import network.address.TCPAddress;
import role.Node;
import testframework.SystemMonitor;
import testframework.TestFramework;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Ping-Pong test for Java TCP Sockets. Initiates the tests on a single JVM, used for easy debugging.
 */
public class SocketMainSingleJVM {

    public static void main(String[] args) throws IOException, InterruptedException, MPIException {
        SystemMonitor sysInfo = SystemMonitor.collectEvery(500, TimeUnit.MILLISECONDS);

        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        GlobalConfig.getInstance().initTCP(true);

        InetAddress ip = TCPAddress.resolveIpAddress();

        /* Start pinger and pongers */
        for (int i = 1; i < totalNodes; i++) {  // first index will be reserved to pinger
            Node ponger = new Node(new TCPAddress(ip, 0));
        }
        Node pinger = new Node(new TCPAddress(ip, 0));

        /* start tests */
        TestFramework testFramework = TestFramework.doPingTests(pinger, totalNodes);

        /* send end signal to all nodes */
        pinger.signalEndToAll();

        GlobalConfig.getInstance().end();

        testFramework.printAllOnConsole();

        sysInfo.printOnConsole();
    }
}
