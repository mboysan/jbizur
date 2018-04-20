package role;

import config.GlobalConfig;
import network.address.MockAddress;
import network.messenger.MessageReceiverMock;
import network.messenger.MessageSenderMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class BizurNodeTest {

    private static int NODE_COUNT = 3;
    private BizurNode[] bizurNodes = new BizurNode[NODE_COUNT];

    private final MessageSenderMock messageSenderMock = new MessageSenderMock();

    @Before
    public void setUp() throws Exception {
        GlobalConfig.getInstance().initTCP(true);

        for (int i = 0; i < NODE_COUNT; i++) {
            bizurNodes[i] = new BizurNode(
                    new MockAddress(UUID.randomUUID().toString()),
                    messageSenderMock,
                    new MessageReceiverMock(),
                    new CountDownLatch(0)
            );
            messageSenderMock.registerRole(bizurNodes[i].getAddress().toString(), bizurNodes[i]);
        }
    }

    @After
    public void tearDown() {
        GlobalConfig.getInstance().reset();
    }

    @Test
    public void startElectionTest() {
        for (int i = 0; i < bizurNodes.length; i++) {
            bizurNodes[i].startElection();
            if(i==0){
                Assert.assertTrue(bizurNodes[i].isLeader());
            } else {
                Assert.assertFalse(bizurNodes[i].isLeader());
            }
        }

/*
        BizurNode bizurNode = bizurNodes[0];

        bizurNode.startElection();
        for (BizurNode role : bizurNodes) {
            if (role == bizurNode){
                Assert.assertTrue(role.isLeader());
            } else {
                Assert.assertFalse(role.isLeader());
            }
        }*/
    }

    @Test
    public void keyValueTest() {
        BizurNode bizurNode = bizurNodes[0];

        String expKey = UUID.randomUUID().toString();
        String expVal = UUID.randomUUID().toString();

        bizurNode.set(expKey, expVal);

        String actVal = bizurNode.get(expKey);
        Assert.assertEquals(expVal, actVal);



        BizurNode bizurNode2 = bizurNodes[1];

        expKey = UUID.randomUUID().toString();
        expVal = UUID.randomUUID().toString();

        bizurNode2.set(expKey, expVal);

        actVal = bizurNode2.get(expKey);
        Assert.assertEquals(expVal, actVal);
    }

    @Test
    public void handleMessage() {
    }
}