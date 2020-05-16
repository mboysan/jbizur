package ee.ut.jbizur.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public final class CoreConf extends GenCoreConf {

    private static final Logger logger = LoggerFactory.getLogger(CoreConf.class);

    private static CoreConf sInstance;

    private CoreConf(Config c){
        super(c);
    }

    public static synchronized void set(String resource) {
        set(new File(Objects.requireNonNull(CoreConf.class.getClassLoader().getResource(resource)).getFile()));
    }

    public static synchronized void set(File file) {
        logger.info("setting configuration from file={}", file);
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.parseFile(file);
        set(config);
    }

    static synchronized void set(Config config) {
        logger.info("setting configuration from config={}", config);
        ConfigFactory.invalidateCaches();
        sInstance = new CoreConf(config);
    }

    public static synchronized GenCoreConf get() {
        if (sInstance == null) {
            logger.info("using default configuration");
            sInstance = new CoreConf(defaultConf());
        }
        return sInstance;
    }

    private static synchronized Config defaultConf() {
        ConfigFactory.invalidateCaches();
        return ConfigFactory.defaultApplication();
    }
}
