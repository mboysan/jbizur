package ee.ut.bench;

import ee.ut.bench.db.BizurClientWrapper;
import ee.ut.bench.tests.AbstractTest;
import ee.ut.bench.tests.IResultSet;
import ee.ut.bench.tests.LatencyTest;
import ee.ut.bench.tests.ThroughputTest;
import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.db.DBOperation;
import ee.ut.bench.db.DBWrapperFactory;

public class TestInit {

    private final AbstractDBClientWrapper dbWrapper;

    public TestInit(String[] args) throws Exception {
        ThroughputTest.OPERATION_COUNT = 5;
        LatencyTest.OPERATION_COUNT = 5;
        this.dbWrapper = DBWrapperFactory.buildAndInit(BizurClientWrapper.class, args);
    }

    void warmup() {
        System.out.println("----------- Warming up " + dbWrapper.toString());

        int backupLatOpCount = LatencyTest.OPERATION_COUNT;
        int backupTputOpCount = ThroughputTest.OPERATION_COUNT;

        LatencyTest.OPERATION_COUNT = 1;
        ThroughputTest.OPERATION_COUNT = 1;

        AbstractTest latTest = new LatencyTest(dbWrapper, DBOperation.RANDOM);
        latTest.run();
        latTest.runParallel();

        AbstractTest tputTest = new ThroughputTest(dbWrapper, DBOperation.RANDOM);
        tputTest.run();
        tputTest.runParallel();

        dbWrapper.reset();

        LatencyTest.OPERATION_COUNT = backupLatOpCount;
        ThroughputTest.OPERATION_COUNT = backupTputOpCount;

        System.out.println("----------- Warming up done for " + dbWrapper.toString());
    }

    void runThroughputTest() {
        AbstractTest tputTest = new ThroughputTest(dbWrapper, DBOperation.DEFAULT);
        IResultSet tputResultSet = tputTest.runParallel();
        System.out.println("----------- Parallel Throughput Test results for " + dbWrapper.toString());
        tputResultSet.print();
    }

    void runLatencyTest() {
        AbstractTest latTest = new LatencyTest(dbWrapper, DBOperation.DEFAULT);
        IResultSet latResultSet = latTest.runParallel();
        System.out.println("----------- Parallel Latency Test results for " + dbWrapper.toString());
        latResultSet.print();
    }

    public static void main(String[] args) throws Exception {
        TestInit testInit = new TestInit(args);

        testInit.warmup();

        testInit.runThroughputTest();
        testInit.dbWrapper.reset();
        testInit.runLatencyTest();
    }
}
