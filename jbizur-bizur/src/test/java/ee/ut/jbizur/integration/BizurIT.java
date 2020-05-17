package ee.ut.jbizur.integration;

import ee.ut.jbizur.config.BizurConf;
import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.TCPAddress;
import ee.ut.jbizur.role.BizurBuilder;
import ee.ut.jbizur.role.BizurClient;
import ee.ut.jbizur.role.BizurMap;
import ee.ut.jbizur.role.BizurNode;
import ee.ut.jbizur.util.MultiThreadExecutor;
import ee.ut.jbizur.util.TestUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class BizurIT {
    private static final Logger logger = LoggerFactory.getLogger(BizurIT.class);

    private static final String MAP = "test-map";

    @Parameterized.Parameters(name = "conf={0}")
    public static Object[][] conf() {
        return new Object[][]{
                {"BizurIT.static.custom.conf"},
                {"BizurIT.static.rapidoid.conf"},
                {"BizurIT.static.netty.conf"},
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

        // let Bizur handle Bucket leaders internally
//        electBucketLeaders();
    }

    private void initConfiguration() {
        // set the configuration
        BizurConf.set(confName);

        memberCount = CoreConf.get().members.size();
        memberAddresses = CoreConf.get().members.stream()
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
        for (int i = 0; i < CoreConf.get().members.size(); i++) {
            BizurBuilder builder = BizurBuilder.builder()
                    .withMemberId(CoreConf.get().members.get(i).id)
                    .withAddress(TCPAddress.resolveTCPAddress(CoreConf.get().members.get(i).tcpAddress));
            if (!CoreConf.get().network.multicast.enabled) {
                // member discovery is disabled so take addresses from the static set.
                builder.withMemberAddresses(memberAddresses);
            }
            nodes[i] = builder.build();
        }
    }

    protected void initClient() throws IOException {
        BizurBuilder builder = BizurBuilder.builder()
                .withMemberId(CoreConf.get().clients.get(0).id)
                .withAddress(TCPAddress.resolveTCPAddress(CoreConf.get().clients.get(0).tcpAddress));
        if (!CoreConf.get().network.multicast.enabled) {
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

    @Deprecated
    private void electBucketLeaders() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // this method must be used for debug purposes
        int bucketCount = BizurConf.get().bizur.bucketCount;
        for (int i = 0; i < bucketCount; i++) {
            Method method = BizurMap.class.getDeclaredMethod("startElection", int.class);
            method.setAccessible(true);
            BizurMap map = nodes[i % nodes.length].getMap(MAP);
            method.invoke(map, i);
            method.setAccessible(false);
        }
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
        String expKey = TestUtil.getRandomString();
        String expVal = TestUtil.getRandomString();

        Assert.assertNull(client.getMap(MAP).put(expKey, expVal));
        Assert.assertEquals(expVal, client.getMap(MAP).get(expKey));
        Assert.assertEquals(expVal, client.getMap(MAP).remove(expKey));
        Assert.assertNull(client.getMap(MAP).get(expKey));
    }

    /**
     * Tests for simple iterate keys operation.
     */
    @Test
    public void simpleIterateKeysTest() {
        Map<Serializable, Serializable> expKeyValMap = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            String expKey = TestUtil.getRandomString();
            String expVal = TestUtil.getRandomString();
            expKeyValMap.put(expKey, expVal);
            Assert.assertNull(client.getMap(MAP).put(expKey, expVal));
            Assert.assertEquals(expVal, client.getMap(MAP).get(expKey));
        }

        for (Serializable actKey : client.getMap(MAP).keySet()) {
            Serializable expVal = expKeyValMap.get(actKey);
            Assert.assertEquals(expVal, client.getMap(MAP).get(actKey));
        }
    }

    /**
     * Tests for set/get operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueSetGetMultiThreadTest() throws Throwable {
        int testCount = 100;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = TestUtil.getRandomString();
                String expVal = TestUtil.getRandomString();

                Assert.assertNull(client.getMap(MAP).put(testKey, expVal));
                Assert.assertEquals(expVal, client.getMap(MAP).get(testKey));
            });
        }
        executor.endExecution();
    }
}
