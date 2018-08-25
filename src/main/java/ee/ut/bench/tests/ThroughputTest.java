package ee.ut.bench.tests;

import ee.ut.bench.util.AbstractDBWrapper;
import ee.ut.bench.util.DBOperation;

public class ThroughputTest extends AbstractTest {

    public static int OPERATION_COUNT = 5;

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
        return null;
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
    }
}
