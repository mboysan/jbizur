package ee.ut.jbizur.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public final class CoreConf {

    private static final Logger logger = LoggerFactory.getLogger(CoreConf.class);

    private static GenCoreConf instance;
    static {
        setDefaultConfig();
    }

    private CoreConf(){}

    public synchronized static void setDefaultConfig() {
        logger.info("setting default configuration.");
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.defaultApplication();
        instance = new GenCoreConf(config);
    }

    public synchronized static void setConfig(String resource) {
        setConfig(new File(Objects.requireNonNull(CoreConf.class.getClassLoader().getResource(resource)).getFile()));
    }

    public synchronized static void setConfig(File file) {
        logger.info("setting configuration from file={}", file);
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.parseFile(file);
        instance = new GenCoreConf(config);
    }

    public static GenCoreConf get() {
        Objects.requireNonNull(instance);
        return instance;
    }
}
