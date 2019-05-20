package ee.ut.jbizur.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class Conf {

    private static JbizurConfig instance;
    static {
        setConfig(new File(Conf.class.getClassLoader().getResource("jbizur.conf").getFile()));
        LogConf.configureLogger();
    }

    public synchronized static void setConfig(File file) {
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.parseFile(file);
        instance = new JbizurConfig(config);
        LogConf.configureLogger();
    }

    public static JbizurConfig get() {
        return instance;
    }
}
