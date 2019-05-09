package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.ClientTestConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MockAddress;
import ee.ut.jbizur.network.address.MockMulticastAddress;
import org.junit.Assert;
import org.junit.Test;
import utils.MultiThreadExecutor;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BizurClientTest extends BizurNodeTestBase {

    private BizurClient[] bizurClients;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createClients();
        registerRolesToClients();
        registerClientsToRoles();
        startClients();
    }

    /**
     * Initializes the Bizur Clients.
     * @throws InterruptedException in case of initialization errors.
     */
    private void createClients() throws InterruptedException, UnknownHostException {
        int clientCount = ClientTestConfig.getClientCount();
        String[] clients = new String[clientCount];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = "client-" + i;
        }
        bizurClients = new BizurClient[clientCount];
        for (int i = 0; i < bizurClients.length; i++) {
            bizurClients[i] = BizurMockBuilder.mockBuilder()
                    .withMemberId(clients[i])
                    .withMulticastAddress(new MockMulticastAddress("", 0))
                    .withAddress(new MockAddress(clients[i]))
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

    /**
     * Registers clients to bizur nodes and vise versa.
     */
    private void registerRolesToClients() {
        for (BizurClient bizurClient : bizurClients) {
            ((BizurClientMock) bizurClient).registerRoles(bizurNodes);
        }
    }

    private void registerClientsToRoles() {
        for (BizurNode bizurNode : bizurNodes) {
            ((BizurNodeMock) bizurNode).registerRoles(bizurClients);
        }
    }

    private void startClients() {
        CompletableFuture[] futures = new CompletableFuture[bizurClients.length];
        for (int i = 0; i < bizurClients.length; i++) {
            futures[i] = bizurClients[i].start();
        }
        for (CompletableFuture future : futures) {
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
        return bizurClients[inx == -1 ? random.nextInt(bizurClients.length) : inx];
    }
}
