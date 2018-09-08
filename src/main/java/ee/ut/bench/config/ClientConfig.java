package ee.ut.bench.config;

import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.jbizur.network.address.TCPAddress;

public final class ClientConfig extends NodeConfig {

    public static String compileTCPAddress() {
        return compileTCPAddress("%s:%d");
    }

    public static String compileTCPAddress(String format) {
        String ipAddr = getIpAddress();
        int port = getPort();
        return String.format(format, ipAddr, port);
    }

    public static String getIpAddress() {
        return ConfigProperties.getString("client.ip", TCPAddress.resolveIpAddress().getHostAddress());
    }

    public static int getPort() {
        return ConfigProperties.getInt("client.port", getPort(MemberConfig.getMemberCount()));
    }

    public static String getClientId() {
        return ConfigProperties.getString("client.id", "client");
    }

    public static Class<? extends AbstractDBClientWrapper> getDBWrapperClass() {
        try {
            String cName = ConfigProperties.getString("client.dbwrapper.class");
            return (Class<? extends AbstractDBClientWrapper>) Class.forName(cName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
