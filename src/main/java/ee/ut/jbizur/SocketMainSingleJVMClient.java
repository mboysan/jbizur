package ee.ut.jbizur;

import ee.ut.jbizur.config.GlobalConfig;
import mpi.MPIException;
import ee.ut.jbizur.network.address.TCPAddress;
import org.pmw.tinylog.Logger;
import ee.ut.jbizur.role.BizurClient;
import ee.ut.jbizur.role.BizurNode;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Ping-Pong test for Java TCP Sockets. Initiates the tests on a single JVM, used for easy debugging.
 */
public class SocketMainSingleJVMClient {

    public static void main(String[] args) throws IOException, InterruptedException, MPIException {
        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        GlobalConfig.getInstance().initTCP();

        InetAddress ip = TCPAddress.resolveIpAddress();

        /* Start pinger and pongers */
        BizurNode node = null;
        for (int i = 0; i < totalNodes; i++) {  // first index will be reserved to pinger
            node = new BizurNode(new TCPAddress(ip, 0));
        }

        TimeUnit.SECONDS.sleep(5);

        BizurClient client = new BizurClient(new TCPAddress(ip, 0));

        client.set("Hello", "World");

        String val = client.get("Hello");
        Logger.debug("receieved val: " + val);

        Set<String> keys = client.iterateKeys();
        Logger.debug("recv keyset: " + keys.toString());

        client.signalEndToAll();

        GlobalConfig.getInstance().end();
    }
}
