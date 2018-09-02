package ee.ut.bench.tests.integrationtests;

import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.tests.LatencyTest;
import ee.ut.bench.tests.ThroughputTest;
import org.junit.Before;

public abstract class AbstractIntegrationTest {

    public static final int NODE_COUNT = 3;

    protected AbstractDBClientWrapper client;

    @Before
    public void init() throws Exception {
        initNodes();
        client = initClient();
    }

    abstract void initNodes();

    abstract AbstractDBClientWrapper initClient() throws Exception;

    public abstract void runTest();

    public void run() {
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
