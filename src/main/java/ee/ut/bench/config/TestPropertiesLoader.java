package ee.ut.bench.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class TestPropertiesLoader {

    private static Properties PROPERTIES = loadProperties();

    private static Properties loadProperties() {
        if (PROPERTIES == null) {
            PROPERTIES = new Properties();
            try {
                InputStream input = TestPropertiesLoader.class.getClassLoader().getResourceAsStream("config.properties");
                PROPERTIES.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return PROPERTIES;
    }

    public static String getString(String key) {
        return getString(key, null);
    }

    public static String getString(String key, String defVal) {
        return PROPERTIES.getProperty(key, defVal);
    }

    public static int getInt(String key) {
        return getInt(key, null);
    }

    public static int getInt(String key, Integer defVal) {
        if (defVal == null) {
            return Integer.parseInt(PROPERTIES.getProperty(key));
        }
        return Integer.parseInt(PROPERTIES.getProperty(key, defVal + ""));
    }
}
