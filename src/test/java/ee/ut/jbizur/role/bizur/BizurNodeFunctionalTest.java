package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.config.BizurTestConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.network.address.Address;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BizurNodeFunctionalTest extends BizurNodeTestBase {

    private Map<String, String> expKeyVals;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        expKeyVals = new ConcurrentHashMap<>();
    }

    /**
     * Tests the leader election flow but when multiple nodes initiate the same procedure at the same time.
     */
    @Test
    @Before
    public void leaderPerBucketElectionCheck() {
        int bucketCount = BizurConfig.getBucketCount();
        for (int i = 0; i < bucketCount; i++) {
            Address otherAddress = getRandomNode().bucketContainer.getBucket(i).getLeaderAddress();
            Assert.assertNotNull(otherAddress);
            int leaderCount = 0;
            for (BizurNode bizurNode : bizurNodes) {
                Bucket localBucket = bizurNode.bucketContainer.getBucket(i);
                Assert.assertTrue(localBucket.getLeaderAddress().isSame(otherAddress));
                if (localBucket.isLeader()) {
                    leaderCount++;
                }
            }
            Assert.assertEquals(1, leaderCount);
        }
    }

    private void localBucketKeyValCheck(BizurNode node, Map<String,String> expKeyVals) throws Exception {
        for (String expKey : expKeyVals.keySet()) {
            String expVal = expKeyVals.get(expKey);
            localBucketKeyValCheck(node, expKey, expVal);
        }
    }

    private void localBucketKeyValCheck(BizurNode node, String expKey, String expVal) throws Exception {
        Assert.assertEquals(expVal, node.bucketContainer.getBucket(hashKey(expKey, node)).getOp(expKey));
    }

    /**
     * Test for sequential set/get operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueSetGetTest() throws Throwable {
        for (int i = 0; i < 10; i++) {
            String expKey = "tkey" + i;
            String expVal = "tval" + i;
            expKeyVals.put(expKey, expVal);

            BizurNode setterNode = getRandomNode();
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode = getRandomNode();
            Assert.assertEquals(expVal, getterNode.get(expKey));
        }

        for (BizurNode bizurNode : bizurNodes) {
            localBucketKeyValCheck(bizurNode, expKeyVals);
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
            int finalI = i;
            runner.execute(() -> {
                String expKey = "tkey" + finalI;
                String expVal = "tval" + finalI;
                expKeyVals.put(expKey, expVal);

                BizurNode setterNode = getRandomNode();
                Assert.assertTrue(setterNode.set(expKey, expVal));

                BizurNode getterNode = getRandomNode();
                String actVal = getterNode.get(expKey);
                Assert.assertEquals(expVal, actVal);
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();

        for (BizurNode bizurNode : bizurNodes) {
            localBucketKeyValCheck(bizurNode, expKeyVals);
        }
    }

    /**
     * Test for sequential set/get/delete operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueDeleteTest() {
        for (int i = 0; i < 10; i++) {
            String expKey = "tkey" + i;
            String expVal = "tval" + i;

            BizurNode setterNode = getRandomNode();
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode1 = getRandomNode();
            Assert.assertEquals(expVal, getterNode1.get(expKey));

            BizurNode deleterNode = getRandomNode();
            Assert.assertTrue(deleterNode.delete(expKey));

            BizurNode getterNode2 = getRandomNode();
            Assert.assertNull(getterNode2.get(expKey));
        }

        for (BizurNode bizurNode : bizurNodes) {
            for (int i = 0; i < BizurTestConfig.getBucketCount(); i++) {
                Assert.assertEquals(0, bizurNode.bucketContainer.getBucket(i).getKeySet().size());
            }
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
            int finalI = i;
            runner.execute(() -> {
                String expKey = "tkey" + finalI;
                String expVal = "tval" + finalI;

                Assert.assertTrue(getRandomNode().set(expKey, expVal));
                Assert.assertTrue(getRandomNode().delete(expKey));
                Assert.assertNull(getRandomNode().get(expKey));
            });
        }
        runner.awaitCompletion();
        runner.throwAnyCaughtException();

        for (BizurNode bizurNode : bizurNodes) {
            for (int i = 0; i < BizurTestConfig.getBucketCount(); i++) {
                Assert.assertEquals(0, bizurNode.bucketContainer.getBucket(i).getKeySet().size());
            }
        }
    }

    /**
     * Tests the iterate keys procedure. Inserts key/val pairs to the bucket. And while inserting,
     * iterates over the inserted keys and compares with the expected values.
     */
    @Test
    public void iterateKeysTest() {
        int keyCount = 10;
        for (int i = 0; i < keyCount; i++) {
            String key = "tkey" + i;
            String val = "tval" + i;

            expKeyVals.put(key, val);
            Assert.assertTrue(getRandomNode().set(key, val));

            Set<String> actKeys = getRandomNode().iterateKeys();

            Assert.assertEquals(expKeyVals.size(), actKeys.size());
            for (String actKey : actKeys) {
                Assert.assertEquals(expKeyVals.get(actKey), getRandomNode().get(actKey));
            }
        }
    }
}