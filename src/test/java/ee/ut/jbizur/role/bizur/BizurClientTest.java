package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.ClientTestConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MockAddress;
import ee.ut.jbizur.network.address.MockMulticastAddress;
import org.junit.Assert;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BizurClientTest extends BizurNodeTestBase {

    protected final Map<String, String> expKeyVals = new HashMap<>();
    protected BizurClient[] bizurClients;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        expKeyVals.clear();
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

    protected Set<Address> getMemberAddresses() {
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
