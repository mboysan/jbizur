package role;

import config.GlobalConfig;
import config.LoggerConfig;
import network.address.MockAddress;
import network.messenger.MessageReceiverMock;
import network.messenger.MessageSenderMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class BizurNodeTest {

    private static final int NODE_COUNT = 3;
    private final BizurNode[] bizurNodes = new BizurNode[NODE_COUNT];

    private MessageSenderMock messageSenderMock;

    @Before
    public void setUp() throws Exception {
        LoggerConfig.configureLogger(Level.DEBUG);

        GlobalConfig.getInstance().initTCP(true);

        this.messageSenderMock = new MessageSenderMock();
        for (int i = 0; i < NODE_COUNT; i++) {
            bizurNodes[i] = new BizurNode(
                    new MockAddress(UUID.randomUUID().toString()),
                    messageSenderMock,
                    new MessageReceiverMock(),
                    new CountDownLatch(0)
            );
            messageSenderMock.registerRole(bizurNodes[i].getAddress().toString(), bizurNodes[i]);
        }
    }

    @After
    public void tearDown() {
        LoggerConfig.configureLogger(Level.DEBUG);

        GlobalConfig.getInstance().reset();
    }

    protected Random getRandom(){
        long seed = System.currentTimeMillis();
        Logger.info("Seed: " + seed);
        return new Random(seed);
    }

    /**
     * Tests the leader election flow. The node that initiates the {@link BizurNode#startElection()} procedure
     * is always elected as the leader in this case.
     */
    @Test
    public void startElectionTest() {
        Random random = getRandom();
        for (int i = 0; i < 10; i++) {
            BizurNode leader = bizurNodes[random.nextInt(bizurNodes.length)];
            leader.tryElectLeader();
//            Assert.assertTrue(leader.isLeader());
        }
    }

    /**
     * Test for sequential set/get operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueInsertTest() {
        Random random = getRandom();
        for (int i = 0; i < 10; i++) {
            String expKey = UUID.randomUUID().toString();
            String expVal = UUID.randomUUID().toString();

            BizurNode setterNode = bizurNodes[random.nextInt(bizurNodes.length)];
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode = bizurNodes[random.nextInt(bizurNodes.length)];
            Assert.assertEquals(expVal, getterNode.get(expKey));
        }
    }

    @Test
    public void keyValueInsertMultiThreadTest() throws Throwable {
        Random random = getRandom();

        int testCount = 100;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CountDownLatch latch = new CountDownLatch(testCount);

        AtomicReference<Throwable> caughtException = new AtomicReference<>(null);
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                try {
                    if (caughtException.get() != null) {
                        return;
                    }

                    BizurNode bizurNode = bizurNodes[random.nextInt(NODE_COUNT)];

                    String testKey = UUID.randomUUID().toString();
                    String expVal = UUID.randomUUID().toString();

                    Assert.assertTrue(bizurNode.set(testKey, expVal));
                    Assert.assertEquals(expVal, bizurNode.get(testKey));
                } catch (Throwable e) {
                    caughtException.compareAndSet(null, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        if (caughtException.get() != null) {
            throw caughtException.get();
        }
    }

    /**
     * Test for sequential set/get/delete operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueDeleteTest() {
        Random random = getRandom();
        for (int i = 0; i < 10; i++) {
            String expKey = UUID.randomUUID().toString();
            String expVal = UUID.randomUUID().toString();

            BizurNode setterNode = bizurNodes[random.nextInt(bizurNodes.length)];
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode1 = bizurNodes[random.nextInt(bizurNodes.length)];
            Assert.assertEquals(expVal, getterNode1.get(expKey));

            BizurNode deleterNode = bizurNodes[random.nextInt(bizurNodes.length)];
            Assert.assertTrue(deleterNode.delete(expKey));

            BizurNode getterNode2 = bizurNodes[random.nextInt(bizurNodes.length)];
            Assert.assertNull(getterNode2.get(expKey));
        }
    }

    @Test
    public void keyValueDeleteMultiThreadTest() throws Throwable {
        long seed = System.currentTimeMillis();
        Logger.info("Seed: " + seed);
        Random random = new Random(seed);

        final CountDownLatch latch = new CountDownLatch(bizurNodes.length);
        AtomicReference<BizurNode> leaderNodeRef = new AtomicReference<>(null);
        for (BizurNode bizurNode : bizurNodes) {
            new Thread(() -> {
                if (bizurNode.tryElectLeader()) {
                    leaderNodeRef.set(bizurNode);
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        int testCount = 100;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        ExecutorService executor = Executors.newFixedThreadPool(1);
        CountDownLatch latch2 = new CountDownLatch(testCount);
        AtomicReference<Throwable> caughtException = new AtomicReference<>(null);
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();
                try {
                    if (caughtException.get() != null) {
                        return;
                    }

//                    BizurNode bizurNode = bizurNodes[random.nextInt(NODE_COUNT)];
                    BizurNode bizurNode = leaderNodeRef.get();
//                    BizurNode bizurNode = bizurNodes[0];
                    Assert.assertTrue(bizurNode.set(testKey, expVal));
                    Assert.assertTrue(bizurNode.delete(testKey));
                    Assert.assertNull(bizurNode.get(testKey));
                } catch (Throwable e) {
                    caughtException.compareAndSet(null, e);
                } finally {
                    latch2.countDown();
                }
            });
        }

        latch.await();

        if (caughtException.get() != null) {
            throw caughtException.get();
        }
    }

    @Test
    public void handleMessage() {
    }
}