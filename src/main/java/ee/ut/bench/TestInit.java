package ee.ut.bench;

import ee.ut.bench.tests.AbstractTest;
import ee.ut.bench.tests.IResultSet;
import ee.ut.bench.tests.LatencyTest;
import ee.ut.bench.tests.ThroughputTest;
import ee.ut.bench.util.BizurWrapper;
import ee.ut.bench.util.DBOperation;
import ee.ut.bench.util.DBWrapperFactory;
import ee.ut.bench.util.AbstractDBWrapper;

public class TestInit {

    private final AbstractDBWrapper dbWrapper;

    public TestInit(String[] args) throws Exception {
        ThroughputTest.OPERATION_COUNT = 5;
        LatencyTest.OPERATION_COUNT = 5;
        this.dbWrapper = DBWrapperFactory.buildAndInit(BizurWrapper.class, args);
    }

    void runThroughputTest() {
        AbstractTest tputTest = new ThroughputTest(dbWrapper, DBOperation.DEFAULT);
        IResultSet tputResultSet = tputTest.run();
        System.out.println("----------- Throughput Test results for " + dbWrapper.toString());
        tputResultSet.print();
    }

    void runLatencyTest() {
        AbstractTest latTest = new LatencyTest(dbWrapper, DBOperation.DEFAULT);
        IResultSet latResultSet = latTest.run();
        System.out.println("----------- Latency Test results for " + dbWrapper.toString());
        latResultSet.print();
    }

    public static void main(String[] args) throws Exception {
        TestInit testInit = new TestInit(args);

        testInit.runThroughputTest();
        testInit.dbWrapper.reset();
        testInit.runLatencyTest();
    }
}
