package ee.ut.jbizur.config;

public class GenBizurConf {
  public final GenBizurConf.Bizur bizur;
  public static class Bizur {
    public final int bucketCount;
    public final int bucketElectRetryCount;
    public final long bucketLockTimeoutMs;
    public final long maxElectionWaitSec;
    
    public Bizur(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.bucketCount = c.hasPathOrNull("bucketCount") ? c.getInt("bucketCount") : 5;
      this.bucketElectRetryCount = c.hasPathOrNull("bucketElectRetryCount") ? c.getInt("bucketElectRetryCount") : 5;
      this.bucketLockTimeoutMs = c.hasPathOrNull("bucketLockTimeoutMs") ? c.getLong("bucketLockTimeoutMs") : 5000;
      this.maxElectionWaitSec = c.hasPathOrNull("maxElectionWaitSec") ? c.getLong("maxElectionWaitSec") : 5;
    }
  }
  
  public GenBizurConf(com.typesafe.config.Config c) {
    final $TsCfgValidator $tsCfgValidator = new $TsCfgValidator();
    final java.lang.String parentPath = "";
    this.bizur = c.hasPathOrNull("bizur") ? new GenBizurConf.Bizur(c.getConfig("bizur"), parentPath + "bizur.", $tsCfgValidator) : new GenBizurConf.Bizur(com.typesafe.config.ConfigFactory.parseString("bizur{}"), parentPath + "bizur.", $tsCfgValidator);
    $tsCfgValidator.validate();
  }
  private static final class $TsCfgValidator  {
    private final java.util.List<java.lang.String> badPaths = new java.util.ArrayList<>();
    
    void addBadPath(java.lang.String path, com.typesafe.config.ConfigException e) {
      badPaths.add("'" + path + "': " + e.getClass().getName() + "(" + e.getMessage() + ")");
    }
    
    void validate() {
      if (!badPaths.isEmpty()) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder("Invalid configuration:");
        for (java.lang.String path : badPaths) {
          sb.append("\n    ").append(path);
        }
        throw new com.typesafe.config.ConfigException(sb.toString()) {};
      }
    }
  }
}