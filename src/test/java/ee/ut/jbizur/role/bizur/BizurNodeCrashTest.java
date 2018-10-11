package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.datastore.bizur.Bucket;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.util.IdUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BizurNodeCrashTest extends BizurNodeTestBase {

    private static final int FIXED_HASH_INDEX = 0;
    private AtomicInteger keyValIdx = new AtomicInteger(0);

    @Before
    public void resetHashIndexes() {
        useHashIndexForAllBucketContainers(-1);
    }

    /**
     * Tests the leader election flow but when multiple nodes initiate the same procedure at the same time.
     */
    @Test
    @Before
    @After
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

    protected void useHashIndexForAllBucketContainers (int index) {
        for (BizurNode bizurNode : bizurNodes) {
            ((BizurNodeMock) bizurNode).hashIndex.set(index);
        }
    }

    @Override
    protected int hashKey(String s) {
        return FIXED_HASH_INDEX;
    }

    /**
     * Tests read/write when a random node failure happens besides the leader.
     */
    @Test
    public void sendFailTest() {
        useHashIndexForAllBucketContainers(FIXED_HASH_INDEX);

        BizurNodeMock leaderOfBucket0 = getLeaderOf(FIXED_HASH_INDEX);
        BizurNodeMock anotherNode = getNextNodeBasedOn(leaderOfBucket0);

        setRandomKeyVals();

        anotherNode.kill();

        setRandomKeyVals();

        anotherNode.revive();

        setRandomKeyVals();

        validateKeyValsForAllNodes();
        validateLocalBucketKeyVals();
    }

    /**
     * Tests read/write when the leader fails.
     */
    @Test
    public void sendFailOnLeaderTest() {
        useHashIndexForAllBucketContainers(FIXED_HASH_INDEX);

        BizurNodeMock leaderOfBucket0 = getLeaderOf(FIXED_HASH_INDEX);
        BizurNodeMock anotherNode = getNextNodeBasedOn(leaderOfBucket0, bizurNodes.length - 1);

        setRandomKeyVals();

        leaderOfBucket0.kill();

        setRandomKeyVals();

        leaderOfBucket0.revive();

        /* NB! the algorithm cannot detect the newly elected leader when the old leader is revived.
           the only time that the previous leader knows about this change is when a write operation
           is performed on another node. */

        // set new key-vals on another node first.
        setRandomKeyVals(anotherNode);
        // set new key-vals on all nodes.
        setRandomKeyVals();

        validateKeyValsForAllNodes();
        validateLocalBucketKeyVals();
    }

    private BizurNodeMock getLeaderOf(int bucketIndex) {
        BizurNodeMock leader = null;
        for (BizurNode bizurNode : bizurNodes) {
            if(bizurNode.bucketContainer.getBucket(bucketIndex).isLeader()) {
                leader = (BizurNodeMock) bizurNode;
                break;
            }
        }
        Assert.assertNotNull(leader);
        return leader;
    }

    private BizurNodeMock getNextNodeBasedOn(BizurNode node, int index) {
        Address nextAddr = IdUtils.nextAddressInUnorderedSet(node.getSettings().getMemberAddresses(), index);
        BizurNodeMock nextNode = null;
        for (BizurNode bizurNode : bizurNodes) {
            if (bizurNode.getSettings().getAddress().isSame(nextAddr)) {
                nextNode = (BizurNodeMock) bizurNode;
                break;
            }
        }
        Assert.assertNotNull(nextNode);
        return nextNode;
    }

    private BizurNodeMock getNextNodeBasedOn(BizurNode leader) {
        BizurNodeMock nextNode = null;
        for (BizurNode bizurNode : bizurNodes) {
            if (bizurNode != leader) {
                nextNode = (BizurNodeMock) bizurNode;
                break;
            }
        }
        Assert.assertNotNull(nextNode);
        return nextNode;
    }

    private void setRandomKeyVals() {
        for (BizurNode bizurNode : bizurNodes) {
            setRandomKeyVals(bizurNode);
        }
    }

    private void setRandomKeyVals(int byNodeId) {
        setRandomKeyVals(getNode(byNodeId));
    }

    private void setRandomKeyVals(BizurNode byNode) {
        String testKey = "tkey" + keyValIdx.get();
        String expVal = "tval" + keyValIdx.get();
        if(byNode instanceof BizurNodeMock && !((BizurNodeMock) byNode).isDead){
            Assert.assertTrue(byNode.set(testKey, expVal));
            expKeyVals.put(testKey, expVal);
        }
        keyValIdx.incrementAndGet();
    }

    private void validateKeyValsForAllNodes() {
        for (BizurNode bizurNode : bizurNodes) {
            validateKeyVals(bizurNode);
        }
    }

    private void validateKeyVals(int nodeId) {
        validateKeyVals(getNode(nodeId));
    }

    private void validateKeyVals(BizurNode byNode) {
        Set<String> keys = byNode.iterateKeys();
        for (String key : keys) {
            Assert.assertEquals(expKeyVals.get(key), byNode.get(key));
        }
        Set<String> expKeys = expKeyVals.keySet();
        for (String expKey : expKeys) {
            Assert.assertEquals(expKeyVals.get(expKey), byNode.get(expKey));
        }
    }

    // Method to sort a string alphabetically
    public static String sortString(String inputString) {
        char tempArray[] = inputString.toCharArray();
        Arrays.sort(tempArray);
        return new String(tempArray);
    }

}
