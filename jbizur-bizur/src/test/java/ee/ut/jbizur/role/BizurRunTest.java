package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.commands.net.BucketView;
import ee.ut.jbizur.util.MultiThreadExecutor;
import ee.ut.jbizur.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class BizurRunTest extends BizurNodeTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BizurRunTest.class);

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

    /**
     * Tests for set/get operations at the same time with multiple nodes.
     */
    @Test
    public void keyValueSetGetMultiThreadTest() throws Throwable {
        electBucketLeaders();

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
        electBucketLeaders();

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

    private void electBucketLeaders() {
        int bucketCount = CoreConf.get().consensus.bizur.bucketCount;
        for (int i = 0; i < bucketCount; i++) {
            getRandomNode().startElection(i);
        }
    }

    @Test
    public void testBucketComparison() {
        BucketView b00 = new BucketView().setVerElectId(0).setVerCounter(0);
        BucketView b10 = new BucketView().setVerElectId(1).setVerCounter(0);
        BucketView b11 = new BucketView().setVerElectId(1).setVerCounter(1);

        AtomicReference<BucketView> maxVerBucketView = new AtomicReference<>(null);

        Assert.assertTrue(maxVerBucketView.compareAndSet(null, b10));
        Assert.assertFalse(maxVerBucketView.compareAndSet(null, b10));
        Assert.assertFalse(maxVerBucketView.compareAndSet(null, b11));
        Assert.assertEquals(b10, maxVerBucketView.get());

        Assert.assertTrue(b00.compareTo(b10) < 0);
        Assert.assertTrue(b00.compareTo(b00) == 0);
        Assert.assertTrue(b10.compareTo(b00) > 0);

        Assert.assertTrue(b00.compareTo(maxVerBucketView.get()) < 0);
        Assert.assertTrue(b10.compareTo(maxVerBucketView.get()) == 0);
        Assert.assertTrue(b11.compareTo(maxVerBucketView.get()) > 0);
    }
}