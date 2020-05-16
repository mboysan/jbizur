package ee.ut.jbizur.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class BizurConf extends GenBizurConf {
    private static final Logger logger = LoggerFactory.getLogger(BizurConf.class);

    private static BizurConf sInstance = null;

    private BizurConf(Config c) {
        super(c);
        CoreConf.set(c);    // we set the CoreConf with the same configuration
    }

    public synchronized static void set(String resource) {
        set(new File(Objects.requireNonNull(CoreConf.class.getClassLoader().getResource(resource)).getFile()));
    }

    public synchronized static void set(File file) {
        logger.info("setting configuration from file={}", file);
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.parseFile(file);
        sInstance = new BizurConf(config);
    }

    public synchronized static BizurConf get() {
        if (sInstance == null) {
            logger.info("using default configuration");
            sInstance = new BizurConf(defaultConf());
        }
        return sInstance;
    }

    private synchronized static Config defaultConf() {
        ConfigFactory.invalidateCaches();
        return ConfigFactory.defaultApplication();
    }
}