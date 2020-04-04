package ee.ut.jbizur.integration;

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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MultiThreadExecutor;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class BizurFunctionalIT {
    private static final Logger logger = LoggerFactory.getLogger(BizurFunctionalIT.class);

    @Parameterized.Parameters(name = "conf={0}")
    public static Object[][] conf() {
        return new Object[][]{
                {"BizurIT.functional.conf"},
                {"BizurIT.discovery.conf"},
        };
    }

    @Parameterized.Parameter
    public String confName;

    private int memberCount;
    private Set<Address> memberAddresses;

    protected BizurNode[] nodes;
    protected BizurClient client;

    @Before
    public void setUp() throws Exception {
        initConfiguration();
        initNodes();
        initClient();
        startNodes();
        startClient();
    }

    private void initConfiguration() {
        // set the configuration
        Conf.setConfig(confName);

        memberCount = Conf.get().members.size();
        memberAddresses = Conf.get().members.stream()
                .map(members$Elm -> {
                    try {
                        return TCPAddress.resolveTCPAddress(members$Elm.tcpAddress);
                    } catch (UnknownHostException e) {
                        logger.error(e.getMessage(), e);
                    }
                    return null;
                })
                .collect(Collectors.toSet());
    }

    protected void initNodes() throws IOException {
        nodes = new BizurNode[memberCount];
        for (int i = 0; i < Conf.get().members.size(); i++) {
            BizurBuilder builder = BizurBuilder.builder()
                    .withMemberId(Conf.get().members.get(i).id)
                    .withAddress(TCPAddress.resolveTCPAddress(Conf.get().members.get(i).tcpAddress));
            if (!Conf.get().network.multicast.enabled) {
                // member discovery is disabled so take addresses from the static set.
                builder.withMemberAddresses(memberAddresses);
            }
            nodes[i] = builder.build();
        }
    }

    protected void initClient() throws IOException {
        BizurBuilder builder = BizurBuilder.builder()
                .withMemberId(Conf.get().clients.get(0).id)
                .withAddress(TCPAddress.resolveTCPAddress(Conf.get().clients.get(0).tcpAddress));
        if (!Conf.get().network.multicast.enabled) {
            // member discovery is disabled so take addresses from the static set.
            builder.withMemberAddresses(memberAddresses);
        }
        client = builder.buildClient();
    }

    protected void startNodes() {
        List<CompletableFuture> futures = new ArrayList<>();
        for (BizurNode node : nodes) {
            futures.add(node.start());
        }
        for (CompletableFuture future : futures) {
            future.join();
        }
    }

    protected void startClient() {
        client.start().join();
    }

    @After
    public void tearDown() {
        client.close();
        for (BizurNode node : nodes) {
            node.close();
        }
    }

    /**
     * Tests for simple get and delete operations.
     */
    @Test
    public void simpleSetGetDeleteTest() {
        String expKey = UUID.randomUUID().toString();
        String expVal = UUID.randomUUID().toString();

        Assert.assertTrue(client.set(expKey, expVal));
        Assert.assertEquals(expVal, client.get(expKey));
        Assert.assertTrue(client.delete(expKey));
        Assert.assertNull(client.get(expKey));
    }

    /**
     * Tests for simple iterate keys operation.
     */
    @Test
    public void simpleIterateKeysTest() {
        Map<String, String> expKeyValMap = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            String expKey = UUID.randomUUID().toString();
            String expVal = UUID.randomUUID().toString();
            expKeyValMap.put(expKey, expVal);
            Assert.assertTrue(client.set(expKey, expVal));
            Assert.assertEquals(expVal, client.get(expKey));
        }

        for (String actKey : client.iterateKeys()) {
            String expVal = expKeyValMap.get(actKey);
            Assert.assertEquals(expVal, client.get(actKey));
        }
    }

    /**
     * Tests for set/get operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueSetGetMultiThreadTest() throws Throwable {
        int testCount = 10;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();

                Assert.assertTrue(client.set(testKey, expVal));
                Assert.assertEquals(expVal, client.get(testKey));
            });
        }
        executor.endExecution();
    }
}
