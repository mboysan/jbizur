package ee.ut.jbizur.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

    private static Properties PROPERTIES;
    static {
        loadProperties(PropertiesLoader.class, "jbizur.properties");
        LoggerConfig.configureLogger();
    }

    public synchronized static void loadProperties(File file) {
        PROPERTIES = new Properties();
        try {
            String uri = (new File("user.dir")).toURI().relativize(file.toURI()).getPath();
            InputStream is = new FileInputStream(new File(uri));
            PROPERTIES.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        return getInt(key, 0);
    }

    static int getInt(String key, int defVal) {
        return Integer.parseInt(PROPERTIES.getProperty(key, defVal + ""));
    }

    static long getLong(String key) {
        return getLong(key, 0L);
    }

    static long getLong(String key, long defVal) {
        return Long.parseLong(PROPERTIES.getProperty(key, defVal + ""));
    }
}
