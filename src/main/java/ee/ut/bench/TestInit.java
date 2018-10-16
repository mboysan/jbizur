package ee.ut.bench;

import ee.ut.bench.config.Config;
import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.db.DBOperation;
import ee.ut.bench.db.DBWrapperFactory;
import ee.ut.bench.tests.AbstractTest;
import ee.ut.bench.tests.IResultSet;
import ee.ut.bench.tests.LatencyTest;
import ee.ut.bench.tests.ThroughputTest;

public class TestInit {

    static {
        loadProperties(null);
    }

    private static void loadProperties(String filePath) {
        if (filePath == null) {
            Config.loadPropertiesFromResources("config.properties");
        } else {
            Config.loadPropertiesFromWorkingDir("config.properties");
        }
    }

    private final AbstractDBClientWrapper dbWrapper;

    public TestInit() throws Exception {
        this.dbWrapper = DBWrapperFactory.buildAndInit(Config.getDBWrapperClass());
    }

    void warmup() {
        System.out.println("----------- Warming up " + dbWrapper.toString());

        AbstractTest latTest = new LatencyTest(dbWrapper, DBOperation.RANDOM).configureWarmup();
        latTest.run();
        latTest.runParallel();

        AbstractTest tputTest = new ThroughputTest(dbWrapper, DBOperation.RANDOM).configureWarmup();
        tputTest.run();
        tputTest.runParallel();

        dbWrapper.reset();

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
        if (args.length > 0) {
            //props file path
            loadProperties(args[0]);
        }

        TestInit testInit = new TestInit();

        testInit.warmup();

        testInit.runThroughputTest();
        testInit.dbWrapper.reset();
        testInit.runLatencyTest();
    }
}
