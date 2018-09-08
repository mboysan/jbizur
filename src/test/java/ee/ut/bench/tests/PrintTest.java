package ee.ut.bench.tests;

import ee.ut.bench.db.DBClientWrapperMock;
import org.junit.Test;

public class PrintTest {

    @Test
    public void latencyPrintTest() {
        LatencyTest.OPERATION_COUNT = 100;
        LatencyTest.QUEUE_DEPTH = 5;
        executeAndPrint(new LatencyTest(new DBClientWrapperMock()));
    }

    @Test
    public void throughputPrintTest() {
        ThroughputTest.OPERATION_COUNT = 100;
        ThroughputTest.QUEUE_DEPTH = 5;
        executeAndPrint(new ThroughputTest(new DBClientWrapperMock()));
    }

    private void executeAndPrint(AbstractTest test) {
        IResultSet set = test.run();

        System.out.println("Serial results of " + test.toString());
        set.print();

        System.out.println();

        set = test.runParallel();

        System.out.println("Parallel results of " + test.toString());
        set.print();
    }
}