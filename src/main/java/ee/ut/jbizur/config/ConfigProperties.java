package ee.ut.jbizur.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {

    private static Properties PROPERTIES = loadProperties(ConfigProperties.class, "config.properties");
    static {
        LoggerConfig.configureLogger();
    }

    public synchronized static Properties loadProperties(Class clazz, String resourceName) {
        Properties properties = new Properties();
        try {
            InputStream input = clazz.getClassLoader().getResourceAsStream(resourceName);
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
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
