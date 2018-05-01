package role;

import config.GlobalConfig;
import config.LoggerConfig;
import network.address.MockAddress;
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

    protected static final int NODE_COUNT = 3;
    protected final BizurNode[] bizurNodes = new BizurNode[NODE_COUNT];

    protected MessageSenderMock messageSenderMock;

    @Before
    public void setUp() throws Exception {
        LoggerConfig.configureLogger(Level.DEBUG);

        GlobalConfig.getInstance().initTCP(true);

        this.messageSenderMock = new MessageSenderMock();
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
        LoggerConfig.configureLogger(Level.DEBUG);

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

}
