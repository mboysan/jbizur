package ee.ut.jbizur.config;

public class NodeTestConfig extends NodeConfig {
    static {
        PropertiesLoader.loadProperties(NodeTestConfig.class, "jbizur.properties");
    }

    public static int getMemberCount() {
        return PropertiesLoader.getInt("node.count");
    }

}
