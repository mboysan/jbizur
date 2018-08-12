package ee.ut.jbizur.role;

import ee.ut.jbizur.network.address.MockAddress;
import ee.ut.jbizur.network.messenger.MessageReceiverMock;
import ee.ut.jbizur.network.messenger.MessageSenderMock;
import org.junit.Assert;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class BizurClientTest extends BizurNodeTestBase {

    protected static int NUM_CLIENTS = 3;

    protected final Map<String, String> expKeyVals = new HashMap<>();
    protected BizurClient[] bizurClients;

    public void setUp() throws Exception {
        super.setUp();
        expKeyVals.clear();
        createClients(NUM_CLIENTS);
        registerClients();
    }

    /**
     * Initializes the Bizur Clients.
     * @param count number of clients to initialize.
     * @throws InterruptedException in case of initialization errors.
     */
    private void createClients(int count) throws InterruptedException {
        bizurClients = new BizurClient[count];
        for (int i = 0; i < count; i++) {
            BizurClient bizurClient = new BizurClient(
                    new MockAddress(UUID.randomUUID().toString()),
                    new MessageSenderMock(),
                    new MessageReceiverMock(),
                    new CountDownLatch(0)
            );
            bizurClients[i] = bizurClient;
        }
    }

    /**
     * Registers clients to bizur nodes and vise versa.
     */
    private void registerClients() {
        for (BizurNode bizurNode : bizurNodes) {
            for (BizurClient bizurClient : bizurClients) {
                ((MessageSenderMock)(((BizurNodeMock) bizurNode).messageSender)).registerRole(bizurClient);
                ((MessageSenderMock) bizurClient.messageSender).registerRole(bizurNode);
            }
        }
    }

    /**
     * Tests key/value operations from the Bizur Client.
     */
    @Test
    public void clientOperationsTest() {
        String expKey = UUID.randomUUID().toString();
        String expVal = UUID.randomUUID().toString();

        /* Test set/get */
        Assert.assertTrue(getRandomClient().set(expKey, expVal));
        expKeyVals.put(expKey, expVal);
        Assert.assertEquals(expVal, getRandomClient().get(expKey));

        /* Test iterate keys */
        Set<String> actKeys = getRandomClient().iterateKeys();
        for (String actKey : actKeys) {
            Assert.assertEquals(expKeyVals.get(actKey), getRandomClient().get(actKey));
        }
        for (String key : expKeyVals.keySet()) {
            Assert.assertEquals(expKeyVals.get(key), getRandomClient().get(key));
        }

        /* Test delete/get */
        Assert.assertTrue(getRandomClient().delete(expKey));
        Assert.assertNull(getRandomClient().get(expKey));
    }

    /**
     * Tests multi-threaded set/get operations from the Bizur Client.
     */
    @Test
    public void clientKeyValueSetGetMultiThreadTest() throws Throwable {
        int testCount = 50;
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();

                Assert.assertTrue(getRandomClient().set(testKey, expVal));
                Assert.assertEquals(expVal, getRandomClient().get(testKey));
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();
    }

    /**
     * Tests multi-threaded delete operations from the Bizur Client.
     */
    @Test
    public void clientKeyValueDeleteMultiThreadTest() throws Throwable {
        int testCount = 50;
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();

                Assert.assertTrue(getRandomClient().set(testKey, expVal));
                Assert.assertTrue(getRandomClient().delete(testKey));
                Assert.assertNull(getRandomClient().get(testKey));
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();
    }

    /**
     * @return random bizur client registered in {@link #bizurClients}.
     */
    protected BizurClient getRandomClient() {
        return getClient(-1);
    }

    /**
     * @param inx index of the client to return. if -1, returns random client.
     * @return created bizur client located in {@link #bizurClients}.
     */
    protected BizurClient getClient(int inx) {
        return bizurClients[inx == -1 ? random.nextInt(bizurClients.length) : inx];
    }
}
