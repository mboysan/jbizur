package role;

import config.GlobalConfig;
import config.LoggerConfig;
import network.address.MockAddress;
import network.messenger.IMessageSender;
import network.messenger.MessageReceiverMock;
import network.messenger.MessageSenderMock;
import org.junit.After;
import org.junit.Before;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class BizurNodeTestBase {

    protected Random random = getRandom();

    protected static final int NODE_COUNT = 3;
    protected BizurNode[] bizurNodes;

    @Before
    public void setUp() throws Exception {
        bizurNodes = new BizurNode[NODE_COUNT];

        GlobalConfig.getInstance().initTCP(true);

        LoggerConfig.configureLogger(Level.INFO);

        for (int i = 0; i < NODE_COUNT; i++) {
            bizurNodes[i] = new BizurNodeMock(
                    new MockAddress(UUID.randomUUID().toString()),
                    new MessageSenderMock(),
                    new MessageReceiverMock(),
                    new CountDownLatch(0)
            );
        }

        for (BizurNode bizurNode : bizurNodes) {
            if(bizurNode instanceof BizurNodeMock){
                IMessageSender messageSender = ((BizurNodeMock) bizurNode).messageSender;
                if(messageSender instanceof MessageSenderMock){
                    for (BizurNode bizurNode1 : bizurNodes){
                        ((MessageSenderMock) messageSender).registerRole(bizurNode1);
                    }
                }
            }
        }
    }

    @After
    public void tearDown() {
        GlobalConfig.getInstance().reset();
    }

    protected Random getRandom(){
        long seed = System.currentTimeMillis();
        return getRandom(seed);
    }

    protected Random getRandom(long seed) {
        Logger.info("Seed: " + seed);
        return new Random(seed);
    }

    protected BizurNodeMock getRandomNode() {
        return getNode(-1);
    }

    protected BizurNodeMock getNode(int inx) {
        return (BizurNodeMock) bizurNodes[inx == -1 ? random.nextInt(bizurNodes.length) : inx];
    }

}
