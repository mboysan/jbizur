package ee.ut.bench.tests.integrationtests;

import ee.ut.bench.config.Config;
import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.db.BizurClientWrapper;
import ee.ut.bench.db.DBWrapperFactory;
import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurNode;
import org.junit.Ignore;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

@Ignore
public class BizurIntegrationTest extends AbstractIntegrationTest {

    private static final int NODE_COUNT = BizurConfig.getAnticipatedMemberCount();

    BizurNode[] nodes = new BizurNode[NODE_COUNT];

    @Override
    void initNodes() throws InterruptedException, UnknownHostException {
        CountDownLatch latch = new CountDownLatch(NODE_COUNT);
        for (int i = 0; i < NODE_COUNT; i++) {
            nodes[i] = initNode(i);
            int finalI = i;
            new Thread(() -> {
                nodes[finalI].start().join();
                latch.countDown();
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private BizurNode initNode(int index) throws UnknownHostException, InterruptedException {
        return BizurBuilder.builder()
                .loadPropertiesFrom(Config.class, "jbizur.properties")
                .build();
    }

    @Override
    AbstractDBClientWrapper initClient() throws Exception {
        return DBWrapperFactory.buildAndInit(BizurClientWrapper.class);
    }

    @Test
    @Override
    public void runTest() {
        run();
    }
}
