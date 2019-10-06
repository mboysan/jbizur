package ee.ut.jbizur.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class Conf {

    private static JbizurConfig instance;
    static {
        setConfigFromResources("jbizur.conf");
    }

    public synchronized static void setConfigFromResources(String resourceName) {
        setConfig(new File(Conf.class.getClassLoader().getResource(resourceName).getFile()));
    }

    public synchronized static void setConfig(File file) {
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.parseFile(file);
        instance = new JbizurConfig(config);
    }

    public static JbizurConfig get() {
        return instance;
    }
}
