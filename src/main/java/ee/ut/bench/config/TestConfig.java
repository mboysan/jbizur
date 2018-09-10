package ee.ut.bench.config;

public final class TestConfig {

    public static int getLatencyOperationCount() {
        return TestPropertiesLoader.getInt("test.latency.operationcount");
    }

    public static int getLatencyQueueDepth() {
        return TestPropertiesLoader.getInt("test.latency.queuedepth");
    }

    public static int getThroughputOperationCount() {
        return TestPropertiesLoader.getInt("test.throughput.operationcount");
    }

    public static int getThroughputQueueDepth() {
        return TestPropertiesLoader.getInt("test.throughput.queuedepth");
    }

    public static int getLatencyWarmupOperationCount() {
        return TestPropertiesLoader.getInt("test.latency.warmup.operationcount");
    }

    public static int getLatencyWarmupQueueDepth() {
        return TestPropertiesLoader.getInt("test.latency.warmup.queuedepth");
    }

    public static int getThroughputWarmupOperationCount() {
        return TestPropertiesLoader.getInt("test.throughput.warmup.operationcount");
    }

    public static int getThroughputWarmupQueueDepth() {
        return TestPropertiesLoader.getInt("test.throughput.warmup.queuedepth");
    }
}
