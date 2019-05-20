package ee.ut.jbizur;

import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;

public class InitMainSingleJVMSimple {
    public static void main(String[] args) throws InterruptedException {
//        withClient();

        BizurNode node1 = BizurBuilder.builder()
                .withMemberId("member1")
                .build();
        BizurNode node2 = BizurBuilder.builder()
                .withMemberId("member2")
                .build();
        BizurClient client = BizurBuilder.builder()
                .withMemberId("client")
                .buildClient();

        node1.start().join();
        node2.start().join();
        client.start().join();

        for (int i = 0; i < 100; i++) {
            client.set("key" + i, "val" + i);
        }
        for (int i = 0; i < 100; i++) {
            client.get("key" + i);
        }
        client.set("key", "test val");
        System.out.println("GET TEST: " + client.get("key"));

        client.signalEndToAll();
//        node1.shutdown();
//        node2.shutdown();
        System.out.println();

        Thread.sleep(5000);
        System.out.println();
        System.out.println("WAIT END");
        System.out.println();
    }

    private static void withClient() throws InterruptedException {
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
