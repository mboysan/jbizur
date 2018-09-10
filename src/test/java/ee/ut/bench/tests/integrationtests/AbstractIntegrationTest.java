package ee.ut.bench.tests.integrationtests;

import ee.ut.bench.config.MemberConfig;
import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.tests.LatencyTest;
import ee.ut.bench.tests.ThroughputTest;
import org.junit.Before;

import java.net.UnknownHostException;

public abstract class AbstractIntegrationTest {

    public static final int NODE_COUNT = MemberConfig.getMemberCount();

    protected AbstractDBClientWrapper client;

    @Before
    public void init() throws Exception {
        initNodes();
        client = initClient();
    }

    abstract void initNodes() throws Exception;

    abstract AbstractDBClientWrapper initClient() throws Exception;

    public abstract void runTest();

    protected void clientTest() {
        for (int i = 0; i < 10; i++) {
            client.set("key" + i, "val" + i);
            System.out.println(client.get("key" + i).toString());
        }
    }

    public void run() {
        clientTest();

        LatencyTest latencyTest = new LatencyTest(client);
        latencyTest.run().print();
        client.reset();
        latencyTest.runParallel().print();

        client.reset();

        ThroughputTest throughputTest = new ThroughputTest(client);
        throughputTest.run().print();
        client.reset();
        throughputTest.runParallel().print();
    }
}
