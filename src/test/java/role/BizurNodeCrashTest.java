package role;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;

public class BizurNodeCrashTest extends BizurNodeTestBase {

    Random random = getRandom();

    @Test
    public void sendFailTest() {
        BizurNodeMock bizurNode1 = getNode(0);
        BizurNodeMock bizurNode2 = getNode(1);
        BizurNodeMock bizurNode3 = getNode(2);

        bizurNode1.tryElectLeader();
        Assert.assertTrue(bizurNode1.isLeader());

        for (int i = 0; i < 100; i++) {
            String testKey = UUID.randomUUID().toString();
            String expVal = UUID.randomUUID().toString();

            if(i == 10){
                /* Setup:
                 * - node 1 (leader),2,3 alive
                 * - node 3 message sender broke
                 * - node 3 thinks leader is down
                 * */
                bizurNode3.setMessageSenderFail(true);

                bizurNode1.set(testKey, expVal);
                Assert.assertEquals(expVal, bizurNode2.get(testKey));

                bizurNode3.setMessageSenderFail(false);
            }

        }
    }

    private BizurNodeMock getNode(int inx) {
        return (BizurNodeMock) bizurNodes[inx == -1 ? random.nextInt(bizurNodes.length) : inx];
    }

}
