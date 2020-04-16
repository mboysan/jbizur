package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.util.MultiThreadExecutor;
import ee.ut.jbizur.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class BizurNodeFunctionalTest extends BizurNodeTestBase {

    /**
     * Test for sequential set/get operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueSetGetTest() {
        int testCount = 50;
        for (int i = 0; i < testCount; i++) {
            String expKey = TestUtil.getRandomString();
            String expVal = TestUtil.getRandomString();
            putExpectedKeyValue(expKey, expVal);

            BizurNode setterNode = getRandomNode();
            Assert.assertTrue(setterNode.set(expKey, expVal));

            BizurNode getterNode = getRandomNode();
            Assert.assertEquals(expVal, getterNode.get(expKey));
        }
    }

    @Test
    public void testLeaderResolution() throws ExecutionException, InterruptedException {
        MultiThreadExecutor executor = new MultiThreadExecutor();
        int bucketCount = CoreConf.get().consensus.bizur.bucketCount;
        for (int i = 0; i < bucketCount; i++) {
            for (BizurNode bizurNode : bizurNodes) {
                int finalI = i;
                executor.execute(() -> {
                    bizurNode.startElection(finalI);
                });
            }
        }
        executor.endExecution();

        Map<Integer, Address> leaders = new HashMap<>();
        for (BizurNode bizurNode : bizurNodes) {
            for (int i = 0; i < bucketCount; i++) {
                Address leader = leaders.get(i);
                Bucket bucket = bizurNode.bucketContainer.getOrCreateBucket(i);
                if (leader != null) {
                    Assert.assertEquals(leader, bucket.getLeaderAddress());
                } else {
                    if (bucket.isLeader()) {
                        leaders.put(i, bucket.getLeaderAddress());
                    }
                }
            }
        }
        System.out.println();
    }

    /**
     * Tests for set/get operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueSetGetMultiThreadTest() throws Throwable {
        int testCount = 50;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String expKey = TestUtil.getRandomString();
                String expVal = TestUtil.getRandomString();
                putExpectedKeyValue(expKey, expVal);

                BizurNode setterNode = getRandomNode();
                Assert.assertTrue(setterNode.set(expKey, expVal));

                BizurNode getterNode = getRandomNode();
                String actVal = getterNode.get(expKey);
                Assert.assertEquals(expVal, actVal);
            });
        }
        executor.endExecution();
    }

    /**
     * Test for sequential set/get/delete operations of a set of keys and values on different nodes.
     */
    @Test
    public void keyValueDeleteTest() {
        int testCount = 50;
        for (int i = 0; i < testCount; i++) {
            String expKey = TestUtil.getRandomString();
            String expVal = TestUtil.getRandomString();

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
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                String expKey = TestUtil.getRandomString();
                String expVal = TestUtil.getRandomString();
                putExpectedKeyValue(expKey, expVal);

                Assert.assertTrue(getRandomNode().set(expKey, expVal));
                Assert.assertTrue(getRandomNode().delete(expKey));
                Assert.assertNull(getRandomNode().get(expKey));

                removeExpectedKey(expKey);
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
            String key = TestUtil.getRandomString();
            String val = TestUtil.getRandomString();

            putExpectedKeyValue(key, val);
            Assert.assertTrue(getRandomNode().set(key, val));

            Set<String> actKeys = getRandomNode().iterateKeys();
            Assert.assertEquals(getExpectedKeySet().size(), actKeys.size());
            for (String expKey : getExpectedKeySet()) {
                Assert.assertEquals(getExpectedValue(expKey), getRandomNode().get(expKey));
            }
            for (String actKey : actKeys) {
                Assert.assertEquals(getExpectedValue(actKey), getRandomNode().get(actKey));
            }
        }
    }
}