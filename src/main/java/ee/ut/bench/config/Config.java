package ee.ut.bench.config;

import ee.ut.bench.db.AbstractDBClientWrapper;
import ee.ut.jbizur.network.address.TCPAddress;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class Config {
    private static PropsLocation propsLocation;
    protected static PropsLoader properties;

    public static void loadPropertiesFromResources(String fileName) {
        propsLocation = PropsLocation.RESOURCES;
        propsLocation.path = fileName;

        properties = PropsLoader.loadProperties(Config.class, fileName);
    }
    public static void loadPropertiesFromWorkingDir(String fileName) {
        propsLocation = PropsLocation.WORKINGDIR;
        propsLocation.path = fileName;

        File relFile = new File((new File("user.dir")).toURI().relativize(new File(fileName).toURI()).getPath());
        properties = PropsLoader.loadProperties(relFile);
    }

    public static PropsLocation getPropsLocation() {
        return propsLocation;
    }

    public enum PropsLocation {
        RESOURCES, WORKINGDIR;
        public String path;
    }

    /* ********************************************************************************************************
     * ATOMIX CONFIG
     * ******************************************************************************************************** */

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

    /* ********************************************************************************************************
     * NODE CONFIG
     * ******************************************************************************************************** */

    public static String getMemberId(int idx) {
        return String.format(properties.getString("member.idformat"), idx);
    }

    public static int getMemberCount() {
        return properties.getInt("node.count");
    }

    public static String getIpAddress() {
        return properties.getString("node.ip", TCPAddress.resolveIpAddress().getHostAddress());
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
        return properties.getInt("node.portinit", 5600);
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

    /* ********************************************************************************************************
    * BENCHMARK TEST CONFIG
    * ******************************************************************************************************** */

    public static int getLatencyOperationCount() {
        return properties.getInt("test.latency.operationcount");
    }

    public static int getLatencyQueueDepth() {
        return properties.getInt("test.latency.queuedepth");
    }

    public static int getThroughputOperationCount() {
        return properties.getInt("test.throughput.operationcount");
    }

    public static int getThroughputQueueDepth() {
        return properties.getInt("test.throughput.queuedepth");
    }

    public static int getLatencyWarmupOperationCount() {
        return properties.getInt("test.latency.warmup.operationcount");
    }

    public static int getLatencyWarmupQueueDepth() {
        return properties.getInt("test.latency.warmup.queuedepth");
    }

    public static int getThroughputWarmupOperationCount() {
        return properties.getInt("test.throughput.warmup.operationcount");
    }

    public static int getThroughputWarmupQueueDepth() {
        return properties.getInt("test.throughput.warmup.queuedepth");
    }

    public static Class<? extends AbstractDBClientWrapper> getDBWrapperClass() {
        try {
            String cName = properties.getString("client.dbwrapper.class");
            return (Class<? extends AbstractDBClientWrapper>) Class.forName(cName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }



}
