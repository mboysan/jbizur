package ee.ut.jbizur;

import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurNode;

import java.net.UnknownHostException;

public class InitMainSingleJVM {

    public static void main(String[] args) throws InterruptedException, UnknownHostException {

        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        BizurNode[] nodes = new BizurNode[totalNodes];
        for (int i = 0; i < nodes.length; i++) {  // first index will be reserved to pinger
            nodes[i] = BizurBuilder.builder().build();
        }

    }
}
