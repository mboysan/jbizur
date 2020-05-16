package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.commands.net.BucketView;
import ee.ut.jbizur.util.MultiThreadExecutor;
import ee.ut.jbizur.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class BizurMapTest extends BizurNodeTestBase {

    @Test
    public void kvDifferentMapsTest() {
        BizurMap map0 = getNode(0).getMap("map0");
        BizurMap map1 = getNode(1).getMap("map1");

        Assert.assertNull(map0.put("test-key", "val0"));
        Assert.assertNull(map1.put("test-key", "val0"));
        Assert.assertEquals("val0", map0.put("test-key", "val1"));
        Assert.assertEquals("val0", map1.put("test-key", "val2"));

        Assert.assertEquals("val1", map0.remove("test-key"));

        Assert.assertNull(map0.get("test-key"));
        Assert.assertEquals("val2", map1.get("test-key"));

        Set<Serializable> map0Keys = map0.keySet();
        Assert.assertEquals(0, map0Keys.size());
        Set<Serializable> map1Keys = map1.keySet();
        Assert.assertEquals(1, map1Keys.size());
        Assert.assertEquals("test-key", map1Keys.iterator().next());
    }

    @Test
    public void kvOnDifferentSerializableObjects() {
        Serializable k = 1;
        Serializable v = "121";
        Assert.assertNull(getNode(0).getMap(MAP).put(k, v));
        putExpectedKeyValue(k, v);
        Assert.assertEquals(v, getNode(0).getMap(MAP).get(k));
        assertKeySetEquals(getNode(0));
        Assert.assertEquals(v, getNode(0).getMap(MAP).remove(k));
        removeExpectedKey(k);
    }

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
            Assert.assertNull(setterNode.getMap(MAP).put(expKey, expVal));

            BizurNode getterNode = getRandomNode();
            Assert.assertEquals(expVal, getterNode.getMap(MAP).get(expKey));
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
                Assert.assertNull(setterNode.getMap(MAP).put(expKey, expVal));

                BizurNode getterNode = getRandomNode();
                String actVal = (String) getterNode.getMap(MAP).get(expKey);
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
            Assert.assertNull(setterNode.getMap(MAP).put(expKey, expVal));

            BizurNode getterNode1 = getRandomNode();
            Assert.assertEquals(expVal, getterNode1.getMap(MAP).get(expKey));

            BizurNode deleterNode = getRandomNode();
            Assert.assertEquals(expVal, deleterNode.getMap(MAP).remove(expKey));    // returns prev value

            BizurNode getterNode2 = getRandomNode();
            Assert.assertNull(getterNode2.getMap(MAP).get(expKey));
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

                Assert.assertNull(getRandomNode().getMap(MAP).put(expKey, expVal));
                Assert.assertEquals(expVal, getRandomNode().getMap(MAP).remove(expKey));    // returns prev value
                Assert.assertNull(getRandomNode().getMap(MAP).get(expKey));

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
            Assert.assertNull(getRandomNode().getMap(MAP).put(key, val));

            Set<Serializable> actKeys = getRandomNode().getMap(MAP).keySet();
            Assert.assertEquals(getExpectedKeySet().size(), actKeys.size());
            for (Serializable expKey : getExpectedKeySet()) {
                Assert.assertEquals(getExpectedValue(expKey), getRandomNode().getMap(MAP).get(expKey));
            }
            for (Serializable actKey : actKeys) {
                Assert.assertEquals(getExpectedValue(actKey), getRandomNode().getMap(MAP).get(actKey));
            }
        }
    }

    @Test
    public void testBucketComparison() {
        BucketView<String, String> b00 = new BucketView<String, String>().setVerElectId(0).setVerCounter(0);
        BucketView<String, String> b10 = new BucketView<String, String>().setVerElectId(1).setVerCounter(0);
        BucketView<String, String> b11 = new BucketView<String, String>().setVerElectId(1).setVerCounter(1);

        AtomicReference<BucketView<String, String>> maxVerBucketView = new AtomicReference<>(null);

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