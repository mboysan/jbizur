package ee.ut.bench.tests.througput;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurClient;
import ee.ut.jbizur.role.BizurNode;
import mpi.MPIException;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

public class JBizurThroughputTest {

    public static void main(String[] args) throws IOException, InterruptedException, MPIException {
        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        GlobalConfig.getInstance().initTCP(true);

        InetAddress ip = TCPAddress.resolveIpAddress();

        /* Start pinger and pongers */
        BizurNode node = null;
        for (int i = 0; i < totalNodes; i++) {  // first index will be reserved to pinger
            node = new BizurNode(new TCPAddress(ip, 0));
        }
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
