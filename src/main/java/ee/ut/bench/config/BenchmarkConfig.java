package ee.ut.bench.config;

import ee.ut.bench.db.AbstractDBClientWrapper;

public final class BenchmarkConfig extends Config {
    private static PropsLoader properties;
    public static void loadPropertiesFromResources(String fileName) {
        properties = _loadPropertiesFromResources(fileName);
    }
    public static void loadPropertiesFromWorkingDir(String fileName) {
        properties = _loadPropertiesFromWorkingDir(fileName);
    }

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
