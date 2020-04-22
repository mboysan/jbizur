package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BizurNodeCrashTest extends BizurNodeTestBase {
    static {
        CoreConf.setConfig("BizurUT.crash.conf");
    }

    @Override
    public void setUp() throws IOException {
        super.setUp();

        // we will work on a single bucket
        Assert.assertEquals(1, CoreConf.get().consensus.bizur.bucketCount);
    }

    /**
     * Tests read/write when a random node failure happens besides the leader.
     */
    @Test
    public void replicaCrashTest() {
        BizurNode leader = getNode(0);
        BizurNode replica = getNode(1);

        // we first set key,val pairs by all nodes to have data and leaders elected.
        setRandomKeyVals(leader);

        // kill a non-leader node
        DeadNodeManager.kill(replica);

        // set another set of key-vals by all.
        setRandomKeyValsOnAll();

        // revive the dead non-leader node
        DeadNodeManager.revive(replica);

        // set another set of key-vals.
        setRandomKeyValsOnAll();

        // let post validations do the rest
    }

    /**
     * Tests read/write when the leader fails.
     */
    @Test
    public void leaderCrashTest() {
        BizurNode leader = getNode(0);
        BizurNode replica = getNode(1);

        // we first set key,val pairs by all nodes to have data and leaders elected.
        setRandomKeyVals(leader);

        // kill leader node
        DeadNodeManager.kill(leader);

        // set another set of key-vals.
        setRandomKeyValsOnAll();

        // revive the the leader node
        DeadNodeManager.revive(leader);

         /* NB! the algorithm cannot detect the newly elected leader when the old leader is revived.
           the only time that the previous leader knows about this change is when a write operation
           is performed on another node. */

        // set new key-vals on non-leader node first.
        setRandomKeyVals(replica);
        // set new key-vals on all nodes.
        setRandomKeyValsOnAll();

        // let post validations do the rest
    }

    private void setRandomKeyValsOnAll() {
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

}
