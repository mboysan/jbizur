package ee.ut.jbizur.config;

public class TestConfig {
    static {
        PropertiesLoader.loadProperties(TestConfig.class, "jbizur.properties");
    }

    public static int getKeyValueSetGetTestCount() {
        return PropertiesLoader.getInt("test.functional.keyValueSetGetTest", 50);
    }
    public static int getKeyValueSetGetMultiThreadTestCount() {
        return PropertiesLoader.getInt("test.functional.keyValueSetGetMultiThreadTest", 50);
    }
    public static int getKeyValueDeleteTestCount() {
        return PropertiesLoader.getInt("test.functional.keyValueDeleteTest", 50);
    }
    public static int getKeyValueDeleteMultiThreadTestCount() {
        return PropertiesLoader.getInt("test.functional.keyValueDeleteMultiThreadTest", 50);
    }
    public static int getIterateKeysTestCount() {
        return PropertiesLoader.getInt("test.functional.iterateKeysTest", 50);
    }
}
