package ee.ut.jbizur.config;

import ee.ut.jbizur.network.ConnectionProtocol;
import ee.ut.jbizur.network.address.TCPAddress;

import java.util.UUID;

public class NodeConfig {

    public static String compileMulticastAddress() {
        return compileMulticastAddress("%s:%d");
    }

    public static String compileMulticastAddress(String format) {
        String group = getMulticastGroup();
        int port = getMulticastPort();
        return String.format(format, group, port);
    }

    public static String compileTCPAddress() {
        return compileTCPAddress("%s:%d", 0);
    }

    public static String compileTCPAddress(int idx) {
        return compileTCPAddress("%s:%d", idx);
    }

    public static String compileTCPAddress(String format, int idx) {
        String ip = getIp();
        int port = getPort(idx);
        return String.format(format, ip, port);
    }

    public static String getMulticastGroup() {
        return PropertiesLoader.getString("node.multicastgroup");
    }

    public static int getMulticastPort() {
        return PropertiesLoader.getInt("node.multicastport");
    }

    public static int getInitPort() {
        return PropertiesLoader.getInt("node.portinit", 0);
    }

    public static int getPort(int idx) {
        return getInitPort() + idx;
    }

    public static String getIp() {
        return PropertiesLoader.getString("node.ip", TCPAddress.resolveIpAddress().getHostAddress());
    }

    public static String getMemberId(int idx) {
        String idFormat = PropertiesLoader.getString("member.idformat");
        if (idFormat == null) {
            return UUID.randomUUID().toString();
        }
        return String.format(idFormat, idx);
    }

    public static String[] getMemberIds() {
        String[] arr = new String[getAnticipatedMemberCount()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = getMemberId(i);
        }
        return arr;
    }

    public static String getClientId() {
        return PropertiesLoader.getString("client.id", UUID.randomUUID().toString());
    }

    public static long getMulticastIntervalMs() {
        return PropertiesLoader.getLong("node.multicast.intervalms", 1000);
    }

    public static ConnectionProtocol getConnectionProtocol() {
        return ConnectionProtocol.valueOf(PropertiesLoader.getString("node.connectionprotocol", "TCP"));
    }

    public static int getAnticipatedMemberCount() {
        return PropertiesLoader.getInt("node.count");
    }

    /**
     * @return timeout (in seconds) for responses between the processes.
     */
    public static long getResponseTimeoutSec() {
        return PropertiesLoader.getLong("node.response_timeout_sec", 5);
    }

    /**
     * @return Number of times to retry the failing message.
     */
    public static int getSendFailRetryCount() {
        return PropertiesLoader.getInt("node.send_fail_retry_count", 1);
    }

    /**
     * @return Max interval (in sec) to wait between election cycles.
     */
    public static long getMaxElectionWaitSec() {
        return PropertiesLoader.getLong("node.max_election_wait_sec", 5);
    }
}
