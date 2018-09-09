package ee.ut.jbizur.config;

public class BizurConfig extends NodeConfig {
    
    public static int getBucketCount() {
        return ConfigProperties.getInt("bizur.bucketcount", 1);
    }
}
