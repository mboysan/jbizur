package ee.ut.jbizur;

import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InitMainSingleJVM {

    public static void main(String[] args) throws InterruptedException {

        int totalNodes = 3;
        if(args != null && args.length == 1){
            totalNodes = Integer.parseInt(args[0]);
        }

        BizurNode[] nodes = new BizurNode[totalNodes];
        for (int i = 0; i < nodes.length; i++) {  // first index will be reserved to pinger
            nodes[i] = BizurBuilder.builder()
                    .withMemberId("member" + i)
                    .build();
        }
        List<CompletableFuture> futures = new ArrayList<>();
        for (BizurNode node : nodes) {
            futures.add(node.start());
        }
        for (CompletableFuture future : futures) {
            future.join();
        }

        BizurClient client = BizurBuilder.builder()
                .withMemberId("client")
                .buildClient();
        client.start().join();

        client.set("test key", "test val");
        System.out.println("GET TEST: " + client.get("test key"));

        client.signalEndToAll();

        System.out.println();

    }
}
