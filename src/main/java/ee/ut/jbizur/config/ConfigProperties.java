package ee.ut.jbizur.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {

    private static Properties PROPERTIES;
    static {
        loadProperties(ConfigProperties.class, "config.properties");
        LoggerConfig.configureLogger();
    }

    public synchronized static void loadProperties(Class clazz, String resourceName) {
        PROPERTIES = new Properties();
        try {
            InputStream input = clazz.getClassLoader().getResourceAsStream(resourceName);
            PROPERTIES.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String getString(String key) {
        return getString(key, null);
    }

    static String getString(String key, String defVal) {
        return PROPERTIES.getProperty(key, defVal);
    }

    static int getInt(String key) {
        return getInt(key, null);
    }

    static int getInt(String key, Integer defVal) {
        if (defVal == null) {
            return Integer.parseInt(PROPERTIES.getProperty(key));
        }
        return Integer.parseInt(PROPERTIES.getProperty(key, defVal + ""));
    }

    static long getLong(String key) {
        return getInt(key, null);
    }

    static long getLong(String key, Long defVal) {
        if (defVal == null) {
            return Long.parseLong(PROPERTIES.getProperty(key));
        }
        return Long.parseLong(PROPERTIES.getProperty(key, defVal + ""));
    }
}
