package ee.ut.jbizur.config;

public class BizurTestConfig extends BizurConfig {
    static {
        PropertiesLoader.loadProperties(BizurTestConfig.class, "jbizur.properties");
    }
}
