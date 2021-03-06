package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.RngUtil;
import ee.ut.jbizur.config.BizurConf;
import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.util.MockUtil;
import ee.ut.jbizur.util.MultiThreadExecutor;
import ee.ut.jbizur.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BizurClientMapTest extends BizurNodeTestBase {

    static {
        BizurConf.set("BizurUT.conf");
    }

    private static final int CLIENT_COUNT = CoreConf.get().clients.size();
    private BizurClient[] bizurClients;

    @Override
    public void setUp() throws IOException {
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
            clientAddresses[i] = MockUtil.mockAddress(clients[i]);
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
        String expKey = TestUtil.getRandomString();
        String expVal = TestUtil.getRandomString();

        /* Test set/get */
        Assert.assertNull(getRandomClient().getMap(MAP).put(expKey, expVal));

        putExpectedKeyValue(expKey, expVal);

        Assert.assertEquals(expVal, getRandomClient().getMap(MAP).get(expKey));

        /* Test iterate keys */
        Set<Serializable> actualKey = getRandomClient().getMap(MAP).keySet();
        for (Serializable actKey : actualKey) {
            Assert.assertEquals(getExpectedValue(actKey), getRandomClient().getMap(MAP).get(actKey));
        }
        for (Serializable expectedKey : getExpectedKeySet()) {
            Assert.assertEquals(getExpectedValue(expectedKey), getRandomClient().getMap(MAP).get(expectedKey));
        }

        /* Test delete/get */
        Assert.assertEquals(expVal, getRandomClient().getMap(MAP).remove(expKey));
        Assert.assertNull(getRandomClient().getMap(MAP).get(expKey));

        removeExpectedKey(expKey);
    }

    /**
     * Tests multi-threaded set/get operations from the Bizur Client.
     */
    @Test
    public void clientKeyValueSetGetMultiThreadTest() throws Throwable {
        electBucketLeaders();

        int testCount = 50;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = TestUtil.getRandomString();
                String expVal = TestUtil.getRandomString();
                putExpectedKeyValue(testKey, expVal);

                Assert.assertNull(getRandomClient().getMap(MAP).put(testKey, expVal));
                Assert.assertEquals(expVal, getRandomClient().getMap(MAP).get(testKey));
            });
        }
        executor.endExecution();
    }

    /**
     * Tests multi-threaded delete operations from the Bizur Client.
     */
    @Test
    public void clientKeyValueDeleteMultiThreadTest() throws Throwable {
        electBucketLeaders();

        int testCount = 50;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = TestUtil.getRandomString();
                String expVal = TestUtil.getRandomString();
                putExpectedKeyValue(testKey, expVal);

                Assert.assertNull(getRandomClient().getMap(MAP).put(testKey, expVal));
                Assert.assertEquals(expVal, getRandomClient().getMap(MAP).remove(testKey));
                Assert.assertNull(getRandomClient().getMap(MAP).get(testKey));

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
    public void iterateKeysTest() {
        int keyCount = 50;
        for (int i = 0; i < keyCount; i++) {
            String testKey = TestUtil.getRandomString();
            String expVal = TestUtil.getRandomString();

            Assert.assertNull(getRandomClient().getMap(MAP).put(testKey, expVal));
            putExpectedKeyValue(testKey, expVal);

            Set<Serializable> actKeys = getRandomClient().getMap(MAP).keySet();
            Assert.assertEquals(getExpectedKeySet().size(), actKeys.size());
            for (Serializable expKey : getExpectedKeySet()) {
                Assert.assertEquals(getExpectedValue(expKey), getRandomClient().getMap(MAP).get(expKey));
            }
            for (Serializable actKey : actKeys) {
                Assert.assertEquals(getExpectedValue(actKey), getRandomClient().getMap(MAP).get(actKey));
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
        return bizurClients[inx == -1 ? RngUtil.nextInt(bizurClients.length) : inx];
    }
}
