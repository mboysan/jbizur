package ee.ut.bench.config;

public final class TestConfig {

    public static int getLatencyOperationCount() {
        return ConfigProperties.getInt("test.latency.operationcount");
    }

    public static int getLatencyQueueDepth() {
        return ConfigProperties.getInt("test.latency.queuedepth");
    }

    public static int getThroughputOperationCount() {
        return ConfigProperties.getInt("test.throughput.operationcount");
    }

    public static int getThroughputQueueDepth() {
        return ConfigProperties.getInt("test.throughput.queuedepth");
    }

    public static int getLatencyWarmupOperationCount() {
        return ConfigProperties.getInt("test.latency.warmup.operationcount");
    }

    public static int getLatencyWarmupQueueDepth() {
        return ConfigProperties.getInt("test.latency.warmup.queuedepth");
    }

    public static int getThroughputWarmupOperationCount() {
        return ConfigProperties.getInt("test.throughput.warmup.operationcount");
    }

    public static int getThroughputWarmupQueueDepth() {
        return ConfigProperties.getInt("test.throughput.warmup.queuedepth");
    }
}
