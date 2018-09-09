package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.config.NodeTestConfig;
import ee.ut.jbizur.config.UserSettings;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;
import ee.ut.jbizur.role.bizur.BizurNode;
import org.junit.*;
import utils.RunnerWithExceptionCatcher;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Ignore
public class BizurSingleJvmIntegrationTest {

    protected static final int MEMBER_COUNT = NodeTestConfig.getMemberCount();

    protected BizurNode[] nodes;
    protected BizurClient client;

    Random random = initRandom();

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
                    .withMemberId(NodeTestConfig.getMemberId(i))
                    .withAddress(TCPAddress.resolveTCPAddress(NodeTestConfig.compileTCPAddress()))
                    .withMulticastAddress(MulticastAddress.resolveMulticastAddress(NodeTestConfig.compileMulticastAddress()))
                    .build();
        }
    }

    protected void initClient() throws UnknownHostException, InterruptedException {
        client = BizurBuilder.builder()
                .withMemberId(NodeTestConfig.getClientId())
                .withAddress(TCPAddress.resolveTCPAddress(NodeTestConfig.compileTCPAddress()))
                .withMulticastAddress(MulticastAddress.resolveMulticastAddress(NodeTestConfig.compileMulticastAddress()))
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
    public void tearDown() throws Exception {
        client.signalEndToAll();
    }

    @Test
    public void simpleSetGetDeleteTest() throws Exception {
        String expKey = UUID.randomUUID().toString();
        String expVal = UUID.randomUUID().toString();

        Assert.assertTrue(client.set(expKey, expVal));
        Assert.assertEquals(expVal, client.get(expKey));
        Assert.assertTrue(client.delete(expKey));
        Assert.assertEquals(null, client.get(expKey));
    }

    @Test
    public void simpleIterateKeysTest() throws Exception {
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

        client.set("elect", "leader");

        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();

                Assert.assertTrue(client.set(testKey, expVal));
                Assert.assertEquals(expVal, client.get(testKey));
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();
    }

    public BizurNode getRandomNode() {
        return nodes[random.nextInt(MEMBER_COUNT)];
    }

    public static Random initRandom() {
        long seed = System.currentTimeMillis();
        System.out.println("seed: " + seed);
        return new Random(seed);
    }

}
