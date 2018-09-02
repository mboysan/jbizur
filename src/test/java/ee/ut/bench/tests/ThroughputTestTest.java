package ee.ut.bench.tests;

import ee.ut.bench.db.DBClientWrapperMock;
import org.junit.Assert;
import org.junit.Test;

public class ThroughputTestTest extends AbstractTestBase {

    @Override
    public void setUp() {
        test = new ThroughputTest(new DBClientWrapperMock());
    }

    @Test
    @Override
    public void testRunParallel() {
        changeSettings(100, 23);
        runAndCheck();

        changeSettings(23, 100);
        runAndCheck();

        changeSettings(25, 100);
        runAndCheck();

        changeSettings(100, 25);
        runAndCheck();

        changeSettings(100, 100);
        runAndCheck();

        for (int i = 0; i < 100; i++) {
            changeSettings(random.nextInt(100) + 1, random.nextInt(100) + 1);
            runAndCheck();
        }
    }

    private void runAndCheck() {
        test.dbWrapper.reset();

        int opCount = ThroughputTest.OPERATION_COUNT;
        int queueDepth = ThroughputTest.QUEUE_DEPTH;

        IResultSet resultSet = test.runParallel();
        long[][] data = (long[][]) resultSet.getData();

        int expCount = (int) Math.ceil((double)opCount / (double)queueDepth);

        Assert.assertEquals(expCount, data.length);
        for (long[] datum : data) {
            Assert.assertNotEquals(0, datum[0]);
        }

        checkDBOperationCount(opCount * test.dbOperations.length);
    }

    private static void changeSettings(int opCount, int queueDepth) {
        ThroughputTest.OPERATION_COUNT = opCount;
        ThroughputTest.QUEUE_DEPTH = queueDepth;
    }
}