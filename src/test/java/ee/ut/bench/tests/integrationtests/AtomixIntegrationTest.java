package ee.ut.bench.tests.integrationtests;

import config.UserSettings;
import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.db.AtomixDBClientWrapper;
import ee.ut.bench.db.DBWrapperFactory;
import io.atomix.cluster.Node;
import network.ConnectionProtocol;
import network.address.TCPAddress;
import org.junit.Test;

public class AtomixIntegrationTest extends AbstractIntegrationTest {

    private final UserSettings userSettings = new UserSettings(null, ConnectionProtocol.TCP_CONNECTION);
    private final static int PORT_INIT = 5000;

    private Node[] nodes = new Node[NODE_COUNT];

    @Override
    void initNodes() {
        for (int i = 0; i < NODE_COUNT; i++) {
            String ipAddr = String.format("%s:%s", TCPAddress.resolveIpAddress().getHostAddress(), PORT_INIT + i);
            String id = "node" + i;
            nodes[i] = initNode(id, ipAddr);
        }
    }

    private Node initNode(String id, String addr) {
        return Node.builder()
                .withId(id)
                .withAddress(addr)
                .build();
    }

    @Override
    AbstractDBClientWrapper initClient() throws Exception {
        return DBWrapperFactory.buildAndInit(AtomixDBClientWrapper.class, null);
    }

    @Test
    @Override
    public void runTest() {
        run();
    }
}
