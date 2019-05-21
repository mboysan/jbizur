package ee.ut.jbizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InitMainSingleJVMStress {

    public static void main(String[] args) throws InterruptedException, UnknownHostException, ExecutionException {

        int memberCount = Conf.get().node.member.expectedCount;
        int clientCount = Conf.get().node.client.expectedCount;


        BizurNode[] nodes = new BizurNode[memberCount];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = BizurBuilder.builder().build();
        }

        BizurClient[] clients = new BizurClient[clientCount];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = BizurBuilder.builder().buildClient();
        }

        for (BizurNode node : nodes) {
            node.start().join();
        }
        for (BizurClient client : clients) {
            client.start().join();
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int idx = i % clients.length;
            futures.add(executor.submit(() -> {
                String key = UUID.randomUUID().toString();
                String val = UUID.randomUUID().toString();
                clients[idx].set(key, val);
                clients[idx].get(key);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        clients[0].signalEndToAll();

        System.out.println();

        Thread.sleep(5000);
        System.out.println();
        System.out.println("WAIT END");
        System.out.println();
    }
}
