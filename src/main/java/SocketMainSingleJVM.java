import config.GlobalConfig;
import mpi.MPIException;
import network.address.TCPAddress;
import org.pmw.tinylog.Logger;
import role.BizurNode;
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
        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        GlobalConfig.getInstance().initTCP(true);

        InetAddress ip = TCPAddress.resolveIpAddress();

        /* Start pinger and pongers */
        BizurNode ponger = null;
        for (int i = 1; i < totalNodes; i++) {  // first index will be reserved to pinger
            ponger = new BizurNode(new TCPAddress(ip, 0));
        }
        BizurNode pinger = new BizurNode(new TCPAddress(ip, 0));

//        pinger.startElection();
        pinger.set("Hello", "World");

//        String val = pinger.get("Hello");
        String val = ponger.get("Hello");
        Logger.debug("receieved val: " + val);

        GlobalConfig.getInstance().end();
    }
}
