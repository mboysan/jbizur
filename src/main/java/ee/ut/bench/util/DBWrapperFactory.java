package ee.ut.bench.util;

public abstract class DBWrapperFactory {

    public static AbstractDBWrapper buildAndInit(Class<? extends AbstractDBWrapper> wrapperClass, String... args) throws Exception {
        AbstractDBWrapper dbWrapper = wrapperClass.getConstructor().newInstance();
        dbWrapper.init(args);
        return dbWrapper;
    }
}
