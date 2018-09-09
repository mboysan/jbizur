package ee.ut.jbizur.config;

public class NodeTestConfig extends NodeConfig {
    static {
        ConfigProperties.loadProperties(NodeTestConfig.class, "config.properties");
    }

    public static int getMemberCount() {
        return ConfigProperties.getInt("node.count");
    }

}
