package role;

import org.junit.Assert;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.util.*;

public class BizurNodeFunctionalTest extends BizurNodeTestBase {

    /**
     * Simple test for the leader election flow.
     */
    @Test
    public void leaderElectionTest() {
        Random random = getRandom();

        BizurNode bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
        bizurNode.tryElectLeader();

        int leaderCnt = 0;
        for (BizurNode node : bizurNodes) {
            leaderCnt += node.isLeader() ? 1 : 0;
        }
        Assert.assertEquals(1, leaderCnt);
    }

    /**
     * Tests the leader election flow but when multiple nodes initiate the same procedure at the same time.
     * @throws Throwable any exception caught during lambda function calls.
     */
    @Test
    public void leaderElectionMultiThreadTest() throws Throwable {
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(bizurNodes.length);
        for (BizurNode bizurNode : bizurNodes) {
            runner.execute(() -> {
                bizurNode.tryElectLeader();
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();

        int leaderCnt = 0;
        for (BizurNode node : bizurNodes) {
            leaderCnt += node.isLeader() ? 1 : 0;
        }
        Assert.assertEquals(1, leaderCnt);
    }

    /**
     * Test for sequential set/get operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueSetGetTest() {
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

    /**
     * Tests for set/get operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueSetGetMultiThreadTest() throws Throwable {
        Random random = getRandom();

        int testCount = 50;

        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();
                BizurNode bizurNode;

                bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
                Assert.assertTrue(bizurNode.set(testKey, expVal));

                bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
                Assert.assertEquals(expVal, bizurNode.get(testKey));
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();
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

    /**
     * Tests for set/get/delete operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueDeleteMultiThreadTest() throws Throwable {
        Random random = getRandom();

        int testCount = 50;

        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();
                BizurNode bizurNode;

                bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
                Assert.assertTrue(bizurNode.set(testKey, expVal));

                bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
                Assert.assertTrue(bizurNode.delete(testKey));

                bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
                Assert.assertNull(bizurNode.get(testKey));
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();
    }

    /**
     * Tests the iterate keys procedure. Inserts key/val pairs to the bucket. And while inserting,
     * iterates over the inserted keys and compares with the expected values.
     */
    @Test
    public void iterateKeysTest() {
        Random random = getRandom();

        int keyCount = 10;

        Map<String, String> expKeyVals = new HashMap<>();

        for (int i = 0; i < keyCount; i++) {
            String key = UUID.randomUUID().toString();
            String val = UUID.randomUUID().toString();
            BizurNode bizurNode;

            expKeyVals.put(key, val);

            bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
            bizurNode.set(key, val);

            bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
            Set<String> actKeys = bizurNode.iterateKeys();

            Assert.assertEquals(expKeyVals.size(), actKeys.size());

            for (String actKey : actKeys) {
                bizurNode = bizurNodes[random.nextInt(bizurNodes.length)];
                Assert.assertEquals(expKeyVals.get(actKey), bizurNode.get(actKey));
            }
        }
    }

    /**
     * Tests all the operations by creating as much chaos as possible.
     */
    @Test
    public void chaosTest() throws Throwable {
        //TODO: implement the chaos test.
    }
}