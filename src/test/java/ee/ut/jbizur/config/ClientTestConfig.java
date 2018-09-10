package ee.ut.jbizur.config;

public class ClientTestConfig {
    static {
        PropertiesLoader.loadProperties(NodeTestConfig.class, "jbizur.properties");
    }

    public static int getClientCount() {
        return PropertiesLoader.getInt("client.count", 1);
    }
}
