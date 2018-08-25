package ee.ut.bench.tests;

import ee.ut.bench.util.AbstractDBWrapper;
import ee.ut.bench.util.DBOperation;

public class LatencyTest extends AbstractTest {

    public static int OPERATION_COUNT = 5;

    public LatencyTest(AbstractDBWrapper dbWrapper) {
        super(dbWrapper);
    }

    public LatencyTest(AbstractDBWrapper dbWrapper, DBOperation... dbOperations) {
        super(dbWrapper, dbOperations);
    }

    @Override
    public IResultSet run() {
        long[] lats = new long[OPERATION_COUNT];
        for (int i = 0; i < OPERATION_COUNT; i++) {
            long initTime = System.currentTimeMillis();
            for (DBOperation dbOperation : dbOperations) {
                dbWrapper.run(dbOperation, "k" + i, "v" + i);
            }
            lats[i] = System.currentTimeMillis() - initTime;
        }
        return new LatResultSet(lats);
    }

    @Override
    public IResultSet runParallel() {
        return null;
    }

    private class LatResultSet implements IResultSet {
        private final long[] lats;

        public LatResultSet(long[] lats) {
            this.lats = lats;
        }

        public String toCSV() {
            String nl = String.format("%n");
            StringBuilder sb = new StringBuilder("opNumber" + nl);
            for (int i = 0; i < lats.length; i++) {
                sb.append(i).append(",").append(lats[i]).append(nl);
            }
            return sb.toString();
        }

        @Override
        public void print() {
            System.out.println(toCSV());
        }
    }
}
