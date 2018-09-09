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

    public static String getIp() {
        return ConfigProperties.getString("node.ip", TCPAddress.resolveIpAddress().getHostAddress());
    }

    public static String getMemberId(int idx) {
        String idFormat = ConfigProperties.getString("member.idformat");
        if (idFormat == null) {
            return UUID.randomUUID().toString();
        }
        return String.format(idFormat, idx);
    }

    public static String getClientId() {
        return ConfigProperties.getString("client.id", UUID.randomUUID().toString());
    }

    public static long getMulticastIntervalMs() {
        return ConfigProperties.getLong("node.multicast.intervalms");
    }

    public static ConnectionProtocol getConnectionProtocol() {
        return ConnectionProtocol.valueOf(ConfigProperties.getString("node.connectionprotocol"));
    }

    public static int getAnticipatedMemberCount() {
        return ConfigProperties.getInt("node.count");
    }

    /**
     * @return timeout (in seconds) for responses between the processes.
     */
    public static long getResponseTimeoutSec() {
        return ConfigProperties.getLong("node.response_timeout_sec");
    }

    /**
     * @return Number of times to retry the failing message.
     */
    public static int getSendFailRetryCount() {
        return ConfigProperties.getInt("node.send_fail_retry_count");
    }

    /**
     * @return Max interval (in sec) to wait between election cycles.
     */
    public static long getMaxElectionWaitSec() {
        return ConfigProperties.getLong("node.max_election_wait_sec");
    }
}
