package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.util.TestUtil;
import org.junit.Test;

import java.util.Arrays;

public class BizurNodeCrashTest extends BizurNodeTestBase {
    static {
        CoreConf.setConfig("BizurUT.crash.conf");
    }

    private int nonLeaderNodeCounter = 0;

    /**
     * Tests read/write when a random node failure happens besides the leader.
     */
    @Test
    public void sendFailTest() {
        // we first set key,val pairs by all nodes to have data and leaders elected.
        setRandomKeyVals();

        // we get a random key
        String expKey = getExpectedKeySet().iterator().next();

        BizurNode leader = getLeader(expKey);
        BizurNode nonLeaderNode = getNextNonLeaderNode(expKey);

        // kill a non-leader node
        DeadNodeManager.kill(nonLeaderNode);

        // set another set of key-vals.
        setRandomKeyVals();

        // revive the dead non-leader node
        DeadNodeManager.revive(nonLeaderNode);

        // set another set of key-vals.
        setRandomKeyVals();

        // let post validations do the rest
    }

    /**
     * Tests read/write when the leader fails.
     */
    @Test
    public void sendFailOnLeaderTest() {
        // we first set key,val pairs by all nodes to have data and leaders elected.
        setRandomKeyVals();

        // we get a random key
        String expKey = getExpectedKeySet().iterator().next();

        BizurNode leader = getLeader(expKey);
        BizurNode nonLeaderNode = getNextNonLeaderNode(expKey);

        // kill leader node
        DeadNodeManager.kill(leader);

        // set another set of key-vals.
        setRandomKeyVals();

        // revive the the leader node
        DeadNodeManager.revive(leader);

         /* NB! the algorithm cannot detect the newly elected leader when the old leader is revived.
           the only time that the previous leader knows about this change is when a write operation
           is performed on another node. */

        // set new key-vals on non-leader node first.
        setRandomKeyVals(nonLeaderNode);
        // set new key-vals on all nodes.
        setRandomKeyVals();

        // let post validations do the rest
    }

    private void setRandomKeyVals() {
        for (BizurNode bizurNode : bizurNodes) {
            if (!DeadNodeManager.isDead(bizurNode)) {
                setRandomKeyVals(bizurNode);
            }
        }
    }

    private void setRandomKeyVals(BizurNode byNode) {
        if (DeadNodeManager.isDead(byNode)) {
            // dead node
            throw new IllegalArgumentException("node is dead, cannot set key val, node=" + byNode);
        }
        String expKey = TestUtil.getRandomString();
        String expVal = TestUtil.getRandomString();
        byNode.set(expKey, expVal);
        putExpectedKeyValue(expKey, expVal);
    }

    private BizurNode getLeader(String key) {
        BucketContainer bucketContainer = getRandomNode().bucketContainer;
        Bucket bucket = bucketContainer.getOrCreateBucket(bucketContainer.hashKey(key));
        Address leaderAddress = bucket.getLeaderAddress();
        return Arrays.stream(bizurNodes)
                .filter(bizurNode -> bizurNode.getSettings().getAddress().equals(leaderAddress))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("leader node not found: key=" + key + ", expAddr=" + leaderAddress));
    }

    private BizurNode getNextNonLeaderNode(String key) {
        BizurNode leaderNode = getLeader(key);
        int idx = nonLeaderNodeCounter++ % bizurNodes.length;
        return !bizurNodes[idx].equals(leaderNode)
                ? bizurNodes[idx]
                : getNextNonLeaderNode(key);
    }

}
