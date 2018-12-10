package ee.ut.jbizurexamples;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tests a 3 node bizur cluster with a client and a simple set-get operation.
 */
public class ThreeBizurNodeSetGet {

    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        // list of member addresses.
        List<Address> nodeAddresses = new ArrayList<>();
        nodeAddresses.add(new TCPAddress("127.0.0.1", 8080));   //member 1
        nodeAddresses.add(new TCPAddress("127.0.0.1", 8081));   //member 2
        nodeAddresses.add(new TCPAddress("127.0.0.1", 8082));   //member 3
        Set<Address> nodeAddrAsSet = new HashSet<>(nodeAddresses);

        // multicast address for all the nodes.
        MulticastAddress multicastAddress = new MulticastAddress("230.0.0.1", 54321);

        // number of buckets per node to be used by the bizur algorithm.
        int bucketCount = 5;

        // initialize the nodes (1 to 3).

        BizurNode node1 = BizurBuilder.builder()
                .withMemberId("member1")
                .withAddress(nodeAddresses.get(0))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastAddress(multicastAddress)
                .withNumBuckets(bucketCount)
                .build();

        BizurNode node2 = BizurBuilder.builder()
                .withMemberId("member2")
                .withAddress(nodeAddresses.get(1))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastAddress(multicastAddress)
                .withNumBuckets(bucketCount)
                .build();

        BizurNode node3 = BizurBuilder.builder()
                .withMemberId("member3")
                .withAddress(nodeAddresses.get(2))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastAddress(multicastAddress)
                .withNumBuckets(bucketCount)
                .build();

        // initialize the client.

        BizurClient client = BizurBuilder.builder()
                .withMemberId("client")
                .withAddress(new TCPAddress("127.0.0.1", 8083))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastAddress(multicastAddress)
                .buildClient();

        new Thread(() -> {node1.start().join();}).start();
        new Thread(() -> {node2.start().join();}).start();
        new Thread(() -> {node3.start().join();}).start();
        new Thread(() -> {client.start().join();}).start();

        System.out.println("*************************************************************");
        System.out.println("ThreeBizurNodeSetGet.main(): starting (5 sec delay)...");
        TimeUnit.SECONDS.sleep(5);

        client.set("key1", "value1");
        String val = client.get("key1");

        System.out.println("ThreeBizurNodeSetGet.main(): read value = " + val);

        client.signalEndToAll();

        System.out.println("ThreeBizurNodeSetGet.main(): end!");
        System.out.println("*************************************************************");
    }

}
