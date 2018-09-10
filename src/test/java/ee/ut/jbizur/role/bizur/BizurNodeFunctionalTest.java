package ee.ut.jbizur.role.bizur;

import org.junit.Assert;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BizurNodeFunctionalTest extends BizurNodeTestBase {

    /**
     * Simple test for the leader election flow.
     */
    @Test
    public void leaderElectionTest() {
        for (BizurNode bizurNode : bizurNodes) {
            if (bizurNode.calculateTurn() == 0) {
                bizurNode.resolveLeader();
                break;
            }
        }

        int leaderCnt = 0;
        for (BizurNode node : bizurNodes) {
            leaderCnt += node.isLeader() ? 1 : 0;
        }
        Assert.assertEquals(1, leaderCnt);
    }

    /**
     * Tests if another node can be elected leader when election process is forced.
     */
    @Test
    public void multiLeaderElection() {
        getNode(0).resolveLeader();
        Assert.assertTrue(getNode(0).isLeader());
        getNode(1).tryElectLeader(true);
        Assert.assertTrue(getNode(1).isLeader());
        getNode(2).tryElectLeader(true);
        Assert.assertTrue(getNode(2).isLeader());
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
                bizurNode.resolveLeader();
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
        for (int i = 0; i < 10; i++) {
            String expKey = UUID.randomUUID().toString();
            String expVal = UUID.randomUUID().toString();

            BizurNode setterNode = getRandomNode();
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode = getRandomNode();
            Assert.assertEquals(expVal, getterNode.get(expKey));
        }
    }

    /**
     * Tests for set/get operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueSetGetMultiThreadTest() throws Throwable {
        int testCount = 50;
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();

                Assert.assertTrue(getRandomNode().set(testKey, expVal));
                Assert.assertEquals(expVal, getRandomNode().get(testKey));
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
        for (int i = 0; i < 10; i++) {
            String expKey = UUID.randomUUID().toString();
            String expVal = UUID.randomUUID().toString();

            BizurNode setterNode = getRandomNode();
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode1 = getRandomNode();
            Assert.assertEquals(expVal, getterNode1.get(expKey));

            BizurNode deleterNode = getRandomNode();
            Assert.assertTrue(deleterNode.delete(expKey));

            BizurNode getterNode2 = getRandomNode();
            Assert.assertNull(getterNode2.get(expKey));
        }
    }

    /**
     * Tests for set/get/delete operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueDeleteMultiThreadTest() throws Throwable {
        int testCount = 50;
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                String testKey = UUID.randomUUID().toString();
                String expVal = UUID.randomUUID().toString();

                Assert.assertTrue(getRandomNode().set(testKey, expVal));
                Assert.assertTrue(getRandomNode().delete(testKey));
                Assert.assertNull(getRandomNode().get(testKey));
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
        int keyCount = 10;

        Map<String, String> expKeyVals = new HashMap<>();

        for (int i = 0; i < keyCount; i++) {
            String key = UUID.randomUUID().toString();
            String val = UUID.randomUUID().toString();

            expKeyVals.put(key, val);

            getRandomNode().set(key, val);

            Set<String> actKeys = getRandomNode().iterateKeys();

            Assert.assertEquals(expKeyVals.size(), actKeys.size());

            for (String actKey : actKeys) {
                Assert.assertEquals(expKeyVals.get(actKey), getRandomNode().get(actKey));
            }
        }
    }
}