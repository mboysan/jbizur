package ee.ut.bench.tests;

import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.bench.db.DBOperation;

import java.util.StringJoiner;

public abstract class AbstractTest {

    protected final AbstractDBClientWrapper dbWrapper;
    protected final DBOperation[] dbOperations;

    public AbstractTest(AbstractDBClientWrapper dbWrapper) {
        this(dbWrapper, DBOperation.DEFAULT);
    }

    public AbstractTest(AbstractDBClientWrapper dbWrapper, DBOperation... dbOperations) {
        this.dbWrapper = dbWrapper;
        this.dbOperations = dbOperations;
        configure();
    }

    protected abstract void configure();
    public abstract AbstractTest configureWarmup();

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
