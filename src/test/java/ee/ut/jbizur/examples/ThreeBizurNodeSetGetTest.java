package ee.ut.jbizur.examples;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThreeBizurNodeSetGetTest {

    static {
        Conf.setConfig("jbizur.conf");
    }

    BizurNode node1;
    BizurNode node2;
    BizurNode node3;
    BizurClient client;

    @Before
    public void setUp() throws Exception {
        // list of member addresses.
        List<Address> nodeAddresses = new ArrayList<>();
        nodeAddresses.add(new TCPAddress("127.0.0.1", 8080));   //member 1
        nodeAddresses.add(new TCPAddress("127.0.0.1", 8081));   //member 2
        nodeAddresses.add(new TCPAddress("127.0.0.1", 8082));   //member 3
        Set<Address> nodeAddrAsSet = new HashSet<>(nodeAddresses);

        // initialize the nodes (1 to 3).
        node1 = BizurBuilder.builder()
                .withMemberId("member1")
                .withAddress(nodeAddresses.get(0))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastEnabled(false)
                .build();

        node2 = BizurBuilder.builder()
                .withMemberId("member2")
                .withAddress(nodeAddresses.get(1))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastEnabled(false)
                .build();

        node3 = BizurBuilder.builder()
                .withMemberId("member3")
                .withAddress(nodeAddresses.get(2))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastEnabled(false)
                .build();

        // initialize the client.
        client = BizurBuilder.builder()
                .withMemberId("client")
                .withAddress(new TCPAddress("127.0.0.1", 8083))
                .withMemberAddresses(nodeAddrAsSet)
                .withMulticastEnabled(false)
                .buildClient();

        // start the nodes
        List<Thread> threads = new ArrayList<>();
        threads.add(new Thread(() -> node1.start().join()));
        threads.add(new Thread(() -> node2.start().join()));
        threads.add(new Thread(() -> node3.start().join()));
        threads.add(new Thread(() -> client.start().join()));
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @After
    public void tearDown() throws Exception {
        client.signalEndToAll();
    }

    @Test
    public void simpleSetGetTest() throws IOException, InterruptedException {
        client.set("key1", "value1");
        String val = client.get("key1");
        Assert.assertEquals("value1", val);
    }

}