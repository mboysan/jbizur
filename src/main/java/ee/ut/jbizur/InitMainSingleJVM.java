package ee.ut.jbizur;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurNode;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class InitMainSingleJVM {

    public static void main(String[] args) throws InterruptedException, UnknownHostException {

        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        MulticastAddress multicastAddress = new MulticastAddress("all-systems.mcast.net", 9090);

        GlobalConfig.getInstance().initTCP(multicastAddress);

        InetAddress ip = TCPAddress.resolveIpAddress();

        /* Start pinger and pongers */
        BizurNode[] nodes = new BizurNode[totalNodes];
        for (int i = 0; i < totalNodes; i++) {  // first index will be reserved to pinger
            nodes[i] = new BizurNode(new TCPAddress(ip, 0));
        }

    }
}
