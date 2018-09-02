package ee.ut.bench.db;

public abstract class DBWrapperFactory {

    public static AbstractDBClientWrapper buildAndInit(Class<? extends AbstractDBClientWrapper> wrapperClass, String... args) throws Exception {
        AbstractDBClientWrapper dbWrapper = wrapperClass.getConstructor().newInstance();
        dbWrapper.init(args);
        return dbWrapper;
    }
}
