package ee.ut.bench.config;

public class NodeConfig {
    public static String compileMulticastAddress() {
        return compileMulticastAddress("%s:%d");
    }

    public static String compileMulticastAddress(String format) {
        String group = getMulticastGroup();
        int port = getMulticastPort();
        return String.format(format, group, port);
    }

    public static String getMulticastGroup() {
        return ConfigProperties.getString("node.multicastgroup");
    }

    public static int getMulticastPort() {
        return ConfigProperties.getInt("node.multicastport");
    }

    public static int getInitPort() {
        return ConfigProperties.getInt("node.portinit");
    }

    public static int getPort(int idx) {
        return getInitPort() + idx;
    }
}
