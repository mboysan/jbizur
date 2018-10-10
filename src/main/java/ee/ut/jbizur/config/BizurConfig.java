package ee.ut.jbizur.config;

public class BizurConfig extends NodeConfig {
    
    public static int getBucketCount() {
        return PropertiesLoader.getInt("bizur.bucketcount", 1);
    }

    public static long getBucketSetupTimeoutSec() {
        return PropertiesLoader.getLong("bizur.bucket_setup_timeout_sec", 10);
    }

    public static int getBucketLeaderElectionRetryCount() {
        return PropertiesLoader.getInt("bizur.bucket_elect_retry_count", 5);
    }
}
