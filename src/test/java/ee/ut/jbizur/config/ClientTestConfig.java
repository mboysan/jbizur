package ee.ut.jbizur.config;

public class ClientTestConfig {
    static {
        ConfigProperties.loadProperties(NodeTestConfig.class, "config.properties");
    }

    public static int getClientCount() {
        return ConfigProperties.getInt("client.count", 1);
    }
}
