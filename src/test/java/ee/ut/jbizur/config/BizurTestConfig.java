package ee.ut.jbizur.config;

public class BizurTestConfig extends BizurConfig {
    static {
        ConfigProperties.loadProperties(BizurTestConfig.class, "config.properties");
    }
}
