package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.network.address.Address;
import org.junit.Assert;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BizurNodeFunctionalTest extends BizurNodeTestBase {

    /**
     * Tests the leader election flow but when multiple nodes initiate the same procedure at the same time.
     * @throws Throwable any exception caught during lambda function calls.
     */
    @Test
    public void leaderPerBucketElectionCheck() throws Throwable {
        int bucketCount = BizurConfig.getBucketCount();
        for (int i = 0; i < bucketCount; i++) {
            Address otherAddress = getRandomNode().getLocalBucket(i).getLeaderAddress();
            Assert.assertNotNull(otherAddress);
            int leaderCount = 0;
            for (BizurNode bizurNode : bizurNodes) {
                Bucket localBucket = bizurNode.getLocalBucket(i);
                Assert.assertTrue(localBucket.getLeaderAddress().isSame(otherAddress));
                if (localBucket.isLeader()) {
                    leaderCount++;
                }
            }
            Assert.assertEquals(1, leaderCount);
        }
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
                String actVal = getRandomNode().get(testKey);
                Assert.assertEquals(expVal, actVal);
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