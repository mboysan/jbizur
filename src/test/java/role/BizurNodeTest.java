package role;

import config.GlobalConfig;
import network.address.MockAddress;
import network.messenger.MessageReceiverMock;
import network.messenger.MessageSenderMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class BizurNodeTest {

    private static int NODE_COUNT = 3;
    private BizurNode[] roles = new BizurNode[NODE_COUNT];

    private final MessageSenderMock messageSenderMock = new MessageSenderMock();

    @BeforeClass
    public static void initClass(){
        GlobalConfig.getInstance().initTCP(true);
    }

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < NODE_COUNT; i++) {
            roles[i] = new BizurNode(
                    new MockAddress(UUID.randomUUID().toString()),
                    messageSenderMock,
                    new MessageReceiverMock(),
                    new CountDownLatch(0)
            );
            messageSenderMock.registerRole(roles[i].getAddress().toString(), roles[i]);
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void startElection() {
        BizurNode bizurNode = roles[0];

        bizurNode.startElection();

    }

    @Test
    public void get() {
    }

    @Test
    public void set() {
    }

    @Test
    public void handleMessage() {
    }
}