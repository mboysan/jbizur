package ee.ut.bench.config;

import ee.ut.jbizur.network.address.TCPAddress;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class AtomixConfig extends Config{
    private static PropsLoader properties;
    public static void loadPropertiesFromResources(String fileName) {
        properties = _loadPropertiesFromResources(fileName);
    }
    public static void loadPropertiesFromWorkingDir(String fileName) {
        properties = _loadPropertiesFromWorkingDir(fileName);
    }

    public static void reset() {
        resetSystemDataDir();
        resetPrimitiveDataDir();
    }

    public static void resetSystemDataDir() {
        try {
            String dir = (new File("user.dir")).toURI().relativize(new File(getSystemDataDir()).toURI()).getPath();
            FileUtils.deleteDirectory(new File(dir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void resetPrimitiveDataDir() {
        try {
            String dir = (new File("user.dir")).toURI().relativize(new File(getPrimitiveDataDir()).toURI()).getPath();
            FileUtils.deleteDirectory(new File(dir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSystemDataDir() {
        return properties.getString("atomix.systemdata.dir");
    }

    public static String getPrimitiveDataDir() {
        return properties.getString("atomix.primitivedata.dir");
    }

    public static File getSystemDataDirFor(int memberIndex) {
        return new File(String.format("%s/mem%d", getSystemDataDir(), memberIndex));
    }

    public static File getPrimitiveDataDirFor(int memberIndex) {
        return new File(String.format("%s/mem%d", getPrimitiveDataDir(), memberIndex));
    }

    public static String getMemberId(int idx) {
        return String.format(properties.getString("member.idformat"), idx);
    }

    public static int getMemberCount() {
        return properties.getInt("member.count");
    }

    public static String getIpAddress() {
        return properties.getString("member.ip", TCPAddress.resolveIpAddress().getHostAddress());
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

    public static String compileMulticastAddress() {
        return compileMulticastAddress("%s:%d");
    }

    public static String compileMulticastAddress(String format) {
        String group = getMulticastGroup();
        int port = getMulticastPort();
        return String.format(format, group, port);
    }

    public static String getMulticastGroup() {
        return properties.getString("node.multicastgroup");
    }

    public static int getMulticastPort() {
        return properties.getInt("node.multicastport");
    }

    public static int getInitPort() {
        return properties.getInt("node.portinit");
    }

    public static int getPort(int idx) {
        return getInitPort() + idx;
    }

    public static String compileClientTCPAddress() {
        return compileTCPAddress("%s:%d", getMemberCount());
    }

    public static String getClientIpAddress() {
        return properties.getString("client.ip", TCPAddress.resolveIpAddress().getHostAddress());
    }

    public static int getClientPort() {
        return properties.getInt("client.port", getPort(getMemberCount()));
    }

    public static String getClientId() {
        return properties.getString("client.id", "client");
    }
}
