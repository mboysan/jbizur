package ee.ut.bench.tests.througput;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurClient;
import ee.ut.jbizur.role.BizurNode;
import mpi.MPIException;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

@Deprecated
public class JBizurThroughputTest {

    public static void main(String[] args) throws IOException, InterruptedException, MPIException {
        MulticastAddress multicastAddress = new MulticastAddress("all-systems.mcast.net", 9090);
        GlobalConfig.getInstance().initTCP(false, multicastAddress);

        InetAddress ip = TCPAddress.resolveIpAddress();

        BizurClient client = new BizurClient(new TCPAddress(ip, 0));
//        BizurNode client = new BizurNode(new TCPAddress(ip, 0));

        client.set("init","consensus");

        int opCount = 1_000;

        long[] lats = new long[opCount];
        long[][] ops = new long[opCount][3];

        long start = System.currentTimeMillis();
        for (int i = 0; i < opCount; i++) {
            long initTime = System.currentTimeMillis();
            client.set("k" + i, "v" + i);
            long endTime = System.currentTimeMillis();

            lats[i] = endTime - initTime;
            ops[i] = new long[]{endTime, (endTime - start), (i + 1)};
        }

        print(csvLats(lats));
        print(csvOps(ops));
    }

    static String csvLats(long[] lats) {
        StringBuilder sb = new StringBuilder("lat");
        for (long lat : lats) {
            sb.append(lat).append(String.format("%n"));
        }
        return sb.toString();
    }

    static String csvOps(long[][] ops) {
        String nl = String.format("%n");
        StringBuilder sb = new StringBuilder("timeStamp,spentTime,opCount" + nl);
        for (int i = 0; i < ops.length; i++) {
            for (int i1 = 0; i1 < ops[i].length; i1++) {
                sb.append(ops[i][i1]).append(",");
            }
            sb.append(nl);
        }
        return sb.toString();
    }

    static void print(String results) {
        System.out.println(results);
    }
}
