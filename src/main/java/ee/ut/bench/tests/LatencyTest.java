package ee.ut.bench.tests;

import ee.ut.bench.config.Config;
import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.db.DBOperation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LatencyTest extends AbstractTest {

    public static int OPERATION_COUNT;
    public static int QUEUE_DEPTH;

    public LatencyTest(AbstractDBClientWrapper dbWrapper) {
        super(dbWrapper);
    }

    public LatencyTest(AbstractDBClientWrapper dbWrapper, DBOperation... dbOperations) {
        super(dbWrapper, dbOperations);
    }

    @Override
    protected void configure() {
        OPERATION_COUNT = Config.getLatencyOperationCount();
        QUEUE_DEPTH = Config.getLatencyQueueDepth();
    }

    @Override
    public LatencyTest configureWarmup() {
        OPERATION_COUNT = Config.getLatencyWarmupOperationCount();
        QUEUE_DEPTH = Config.getLatencyWarmupQueueDepth();
        return this;
    }

    @Override
    public IResultSet run() {
        long[][] lats = new long[OPERATION_COUNT][2];
        for (int i = 0; i < OPERATION_COUNT; i++) {
            long initTime = System.currentTimeMillis();
            for (DBOperation dbOperation : dbOperations) {
                dbWrapper.run(dbOperation, "k" + i, "v" + i);
            }
            long endTime = System.currentTimeMillis();
            lats[i] = new long[]{endTime, (endTime - initTime)};
        }
        return new LatResultSet(lats);
    }

    @Override
    public IResultSet runParallel() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        int opsSize = OPERATION_COUNT;
        long[][] lats = new long[opsSize][3];
        AtomicInteger opsInx = new AtomicInteger(0);
        for (int i = OPERATION_COUNT; i > 0; i -= QUEUE_DEPTH) {
            int queueOpCount = i < QUEUE_DEPTH ? i : QUEUE_DEPTH;
            CountDownLatch opLatch = new CountDownLatch(queueOpCount);
            for (int i1 = 0; i1 < queueOpCount; i1++) {
                int finalI = i;
                executor.execute(() -> {
                    long startTime = System.currentTimeMillis();
                    for (DBOperation dbOperation : dbOperations) {
                        dbWrapper.run(dbOperation, "k" + finalI, "v" + finalI);
                    }
                    long endTime = System.currentTimeMillis();
                    lats[opsInx.getAndIncrement()] = new long[]{endTime, (endTime - startTime), queueOpCount};
                    opLatch.countDown();
                });
            }
            try {
                opLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return new LatResultSet(lats);
    }

    private class LatResultSet implements IResultSet {
        private final long[][] lats;

        public LatResultSet(long[][] lats) {
            this.lats = lats;
        }

        public String toCSV() {
            String nl = String.format("%n");
            StringBuilder sb = new StringBuilder("timestamp,opNumber,lat,operations" + nl);
            for (int i = 0; i < lats.length; i++) {
                sb.append(lats[i][0]).append(",").append(i).append(",").append(lats[i][1]).append(",").append(convertDBOperationsToStr()).append(nl);
            }
            return sb.toString();
        }

        @Override
        public void print() {
            System.out.println(toCSV());
        }

        @Override
        public Object getData() {
            return lats;
        }
    }
}
