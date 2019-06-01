package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.util.IdUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class BizurNodeCrashTest extends BizurNodeTestBase {

    private static final int FIXED_HASH_INDEX = 0;
    private AtomicInteger keyValIdx = new AtomicInteger(0);

    @Before
    public void resetHashIndexes() {
        useHashIndexForAllBucketContainers(-1);
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

        setRandomKeyVals();

        BizurNodeMock leaderOfBucket0 = getLeaderOf(FIXED_HASH_INDEX);
        BizurNodeMock anotherNode = getNextNodeBasedOn(leaderOfBucket0);

        setRandomKeyVals();

        anotherNode.kill();

        setRandomKeyVals();

        anotherNode.revive();

        setRandomKeyVals();
    }

    /**
     * Tests read/write when the leader fails.
     */
    @Test
    public void sendFailOnLeaderTest() {
        useHashIndexForAllBucketContainers(FIXED_HASH_INDEX);

        setRandomKeyVals();

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
            if (bizurNode.getSettings().getAddress().equals(nextAddr)) {
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

    private void setRandomKeyVals(BizurNode byNode) {
        String testKey = "tkey" + keyValIdx.get();
        String expVal = "tval" + keyValIdx.get();
        if(byNode instanceof BizurNodeMock && !((BizurNodeMock) byNode).isDead){
            Assert.assertTrue(byNode.set(testKey, expVal));
            putExpectedKeyValue(testKey, expVal);
        }
        keyValIdx.incrementAndGet();
    }
}
