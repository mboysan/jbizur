package ee.ut.bench.tests;

import ee.ut.bench.util.AbstractDBWrapper;
import ee.ut.bench.util.DBOperation;

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
}
