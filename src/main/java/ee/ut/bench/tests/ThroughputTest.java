package ee.ut.bench.tests;

import ee.ut.bench.util.AbstractDBWrapper;
import ee.ut.bench.util.DBOperation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThroughputTest extends AbstractTest {

    public static int OPERATION_COUNT = 5;
    public static int QUEUE_DEPTH = 64;

    public ThroughputTest(AbstractDBWrapper dbWrapper) {
        super(dbWrapper);
    }

    public ThroughputTest(AbstractDBWrapper dbWrapper, DBOperation... dbOperations) {
        super(dbWrapper, dbOperations);
    }

    @Override
    public IResultSet run() {
        long[][] ops = new long[OPERATION_COUNT][3];
        long start = System.currentTimeMillis();
        for (int i = 0; i < OPERATION_COUNT; i++) {
            for (DBOperation dbOperation : dbOperations) {
                dbWrapper.run(dbOperation, "k" + i, "v" + i);
            }
            long endTime = System.currentTimeMillis();
            ops[i] = new long[]{endTime, (endTime - start), (i + 1)};
        }
        return new TPutResultSet(ops);
    }

    @Override
    public IResultSet runParallel() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        int opsSize = (int) Math.ceil((double)OPERATION_COUNT / (double)QUEUE_DEPTH);
        long[][] ops = new long[opsSize][3];
        int opsInx = 0;
        for (int i = OPERATION_COUNT; i > 0; i -= QUEUE_DEPTH) {
            int queueOpCount = i < QUEUE_DEPTH ? i : QUEUE_DEPTH;
            CountDownLatch opLatch = new CountDownLatch(queueOpCount);
            long startTime = System.currentTimeMillis();
            for (int i1 = 0; i1 < queueOpCount; i1++) {
                int finalI = i;
                executor.execute(() -> {
                    for (DBOperation dbOperation : dbOperations) {
                        dbWrapper.run(dbOperation, "k" + finalI, "v" + finalI);
                    }
                    opLatch.countDown();
                });
            }
            try {
                opLatch.await();
                long endTime = System.currentTimeMillis();
                ops[opsInx++] = new long[]{endTime, (endTime - startTime), queueOpCount};
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return new TPutResultSet(ops);
    }

    private class TPutResultSet implements IResultSet {
        private final long[][] ops;

        TPutResultSet(long[][] ops) {
            this.ops = ops;
        }

        public String toCSV() {
            String nl = String.format("%n");
            StringBuilder sb = new StringBuilder("timeStamp,spentTime(ms),opCount" + nl);
            for (int i = 0; i < ops.length; i++) {
                for (int i1 = 0; i1 < ops[i].length; i1++) {
                    sb.append(ops[i][i1]).append(",");
                }
                sb.append(nl);
            }
            return sb.toString();
        }

        @Override
        public void print() {
            System.out.println(toCSV());
        }

        @Override
        public Object getData() {
            return ops;
        }
    }
}
