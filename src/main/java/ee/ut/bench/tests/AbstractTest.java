package ee.ut.bench.tests;

import ee.ut.bench.util.AbstractDBWrapper;
import ee.ut.bench.util.DBOperation;

import java.util.StringJoiner;

public abstract class AbstractTest {

    protected final AbstractDBWrapper dbWrapper;
    protected final DBOperation[] dbOperations;

    public AbstractTest(AbstractDBWrapper dbWrapper) {
        this(dbWrapper, DBOperation.DEFAULT);
    }

    public AbstractTest(AbstractDBWrapper dbWrapper, DBOperation... dbOperations) {
        this.dbWrapper = dbWrapper;
        this.dbOperations = dbOperations;
    }

    public abstract IResultSet run();
    public abstract IResultSet runParallel();

    protected String convertDBOperationsToStr() {
        StringJoiner sj = new StringJoiner("+");
        for (DBOperation dbOperation : dbOperations) {
            sj.add(dbOperation.toString());
        }
        return sj.toString();
    }
}
