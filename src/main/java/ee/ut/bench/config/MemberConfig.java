package ee.ut.bench.config;

import ee.ut.jbizur.network.address.TCPAddress;

public class MemberConfig extends NodeConfig {

    public static String getMemberId(int idx) {
        return String.format(TestPropertiesLoader.getString("member.idformat"), idx);
    }

    public static int getMemberCount() {
        return TestPropertiesLoader.getInt("member.count");
    }

    public static String getIpAddress() {
        return TestPropertiesLoader.getString("member.ip", TCPAddress.resolveIpAddress().getHostAddress());
    }

    public static String compileTCPAddress(int idx) {
        return compileTCPAddress("%s:%d", idx);
    }

    public static String compileTCPAddress(String format, int idx) {
        String ipAddr = getIpAddress();
        int port = getPort(idx);
        return String.format(format, ipAddr, port);
    }

    public static String[] getMemberIds() {
        String[] arr = new String[getMemberCount()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = getMemberId(i);
        }
        return arr;
    }

}
