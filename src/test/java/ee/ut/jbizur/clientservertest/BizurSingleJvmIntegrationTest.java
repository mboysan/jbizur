package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.config.UserSettings;
import ee.ut.jbizur.network.ConnectionProtocol;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurClient;
import ee.ut.jbizur.role.BizurNode;
import org.junit.*;
import utils.RunnerWithExceptionCatcher;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Ignore
public class BizurSingleJvmIntegrationTest {

    protected static final int TOTAL_NODE_COUNT = 3;

    protected UserSettings userSettings;
    protected BizurNode[] nodes;
    protected BizurClient client;

    Random random = initRandom();

    @Before
    public void setUp() throws Exception {
        userSettings = new UserSettings(null, ConnectionProtocol.TCP_CONNECTION);

        GlobalConfig.getInstance().initTCP();

        InetAddress ip = TCPAddress.resolveIpAddress();

        nodes = new BizurNode[TOTAL_NODE_COUNT];
        for (int i = 0; i < TOTAL_NODE_COUNT; i++) {  // first index will be reserved to pinger
            nodes[i] = new BizurNode(new TCPAddress(ip, 0));
        }
        client = new BizurClient(new TCPAddress(ip, 0));
    }

    @After
    public void tearDown() throws Exception {
        client.signalEndToAll();
        GlobalConfig.getInstance().end();
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
        return nodes[random.nextInt(TOTAL_NODE_COUNT)];
    }

    public static Random initRandom() {
        long seed = System.currentTimeMillis();
        System.out.println("seed: " + seed);
        return new Random(seed);
    }

}
