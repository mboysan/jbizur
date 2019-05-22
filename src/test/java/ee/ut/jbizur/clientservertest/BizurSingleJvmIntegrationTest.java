package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;
import org.junit.*;
import utils.MultiThreadExecutor;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Ignore
public class BizurSingleJvmIntegrationTest {
    static {
        Conf.setConfigFromResources("jbizur_integ_test.conf");
    }

    protected static final int MEMBER_COUNT = Conf.get().members.size();

    protected BizurNode[] nodes;
    protected BizurClient client;

    @Before
    public void setUp() throws Exception {
        initNodes();
        initClient();
        startNodes();
        startClient();
    }

    protected void initNodes() throws UnknownHostException, InterruptedException {
        nodes = new BizurNode[MEMBER_COUNT];
        for (int i = 0; i < nodes.length; i++) {

            nodes[i] = BizurBuilder.builder()
                    .withMemberId(String.format(Conf.get().node.member.idFormat, i))
                    .withAddress(TCPAddress.resolveTCPAddress(Conf.get().members.get(i).tcpAddress))
                    .withMulticastAddress(MulticastAddress.resolveMulticastAddress(Conf.get().network.multicast.address))
                    .build();
        }
    }

    protected void initClient() throws UnknownHostException, InterruptedException {
        client = BizurBuilder.builder()
                .withMemberId(Conf.get().clients.get(0).id)
                .withAddress(TCPAddress.resolveTCPAddress(Conf.get().clients.get(0).tcpAddress))
                .withMulticastAddress(MulticastAddress.resolveMulticastAddress(Conf.get().network.multicast.address))
                .buildClient();
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
    public void tearDown() throws InterruptedException {
        client.signalEndToAll();
        Thread.sleep(5000);
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
