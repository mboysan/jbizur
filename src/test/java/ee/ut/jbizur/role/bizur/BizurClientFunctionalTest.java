package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.common.config.Conf;
import ee.ut.jbizur.common.protocol.address.Address;
import org.junit.Assert;
import org.junit.Test;
import util.MockUtils;
import util.MultiThreadExecutor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static util.TestUtils.getRandom;

public class BizurClientFunctionalTest extends BizurNodeTestBase {

    static {
        Conf.setConfig("BizurUT.conf");
    }

    private static final int CLIENT_COUNT = Conf.get().clients.size();
    private BizurClient[] bizurClients;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createClients();
        startClients();
    }

    /**
     * Initializes the Bizur Clients.
     */
    private void createClients() throws IOException {
        String[] clients = new String[CLIENT_COUNT];
        Address[] clientAddresses = new Address[CLIENT_COUNT];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = "client-" + i;
            clientAddresses[i] = MockUtils.mockAddress(clients[i]);
        }
        bizurClients = new BizurClient[CLIENT_COUNT];
        for (int i = 0; i < bizurClients.length; i++) {
            bizurClients[i] = BizurBuilder.builder()
                    .withMemberId(clients[i])
                    .withMulticastEnabled(false)
                    .withAddress(clientAddresses[i])
                    .withMemberAddresses(getMemberAddresses())
                    .buildClient();
        }
    }

    private Set<Address> getMemberAddresses() {
        Set<Address> addressSet = new HashSet<>();
        for (BizurNode bizurNode : bizurNodes) {
            addressSet.add(bizurNode.getSettings().getAddress());
        }
        return addressSet;
    }

    private void startClients() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (BizurClient bizurClient : bizurClients) {
            futures.add(bizurClient.start());
        }
        for (CompletableFuture<Void> future : futures) {
            future.join();
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

        putExpectedKeyValue(expKey, expVal);

        Assert.assertEquals(expVal, getRandomClient().get(expKey));

        /* Test iterate keys */
        Set<String> actualKey = getRandomClient().iterateKeys();
        for (String actKey : actualKey) {
            Assert.assertEquals(getExpectedValue(actKey), getRandomClient().get(actKey));
        }
        for (String expectedKey : getExpectedKeySet()) {
            Assert.assertEquals(getExpectedValue(expectedKey), getRandomClient().get(expectedKey));
        }

        /* Test delete/get */
        Assert.assertTrue(getRandomClient().delete(expKey));
        Assert.assertNull(getRandomClient().get(expKey));

        removeExpectedKey(expKey);
    }

    /**
     * Tests multi-threaded set/get operations from the Bizur Client.
     */
    @Test
    public void clientKeyValueSetGetMultiThreadTest() throws Throwable {
        int testCount = 50;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();
                putExpectedKeyValue(testKey, expVal);

                Assert.assertTrue(getRandomClient().set(testKey, expVal));
                Assert.assertEquals(expVal, getRandomClient().get(testKey));
            });
        }
        executor.endExecution();
    }

    /**
     * Tests multi-threaded delete operations from the Bizur Client.
     */
    @Test
    public void clientKeyValueDeleteMultiThreadTest() throws Throwable {
        int testCount = 50;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();
                putExpectedKeyValue(testKey, expVal);

                Assert.assertTrue(getRandomClient().set(testKey, expVal));
                Assert.assertTrue(getRandomClient().delete(testKey));
                Assert.assertNull(getRandomClient().get(testKey));

                removeExpectedKey(testKey);
            });
        }
        executor.endExecution();
    }

    /**
     * Tests the iterate keys procedure. Inserts key/val pairs to the bucket. And while inserting,
     * iterates over the inserted keys and compares with the expected values.
     */
    @Test
    public void iterateKeysTest() throws ExecutionException, InterruptedException {
        int keyCount = 50;
        for (int i = 0; i < keyCount; i++) {
            String testKey = "tkey" + i;
            String expVal = "tval" + i;

            Assert.assertTrue(getRandomClient().set(testKey, expVal));
            putExpectedKeyValue(testKey, expVal);

            Set<String> actKeys = getRandomClient().iterateKeys();
            Assert.assertEquals(getExpectedKeySet().size(), actKeys.size());
            for (String expKey : getExpectedKeySet()) {
                Assert.assertEquals(getExpectedValue(expKey), getRandomClient().get(expKey));
            }
            for (String actKey : actKeys) {
                Assert.assertEquals(getExpectedValue(actKey), getRandomClient().get(actKey));
            }
        }
    }

    /**
     * @return random bizur client registered in {@link #bizurClients}.
     */
    private BizurClient getRandomClient() {
        return getClient(-1);
    }

    /**
     * @param inx index of the client to return. if -1, returns random client.
     * @return created bizur client located in {@link #bizurClients}.
     */
    private BizurClient getClient(int inx) {
        return bizurClients[inx == -1 ? getRandom().nextInt(bizurClients.length) : inx];
    }
}
