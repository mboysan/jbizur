package ee.ut.bench.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class PropsLoader {

    private Properties properties;

    private PropsLoader() {
    }

    static PropsLoader loadProperties(File file) {
        PropsLoader propsLoader = new PropsLoader();
        Properties properties = new Properties();
        try {
            String uri = (new File("user.dir")).toURI().relativize(file.toURI()).getPath();
            InputStream is = new FileInputStream(new File(uri));
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        propsLoader.properties = properties;
        return propsLoader;
    }

    static PropsLoader loadProperties(Class clazz, String resourceName) {
        PropsLoader propsLoader = new PropsLoader();
        Properties properties = new Properties();
        try {
            InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        propsLoader.properties = properties;
        return propsLoader;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defVal) {
        return properties.getProperty(key, defVal);
    }

    public int getInt(String key) {
        return getInt(key, null);
    }

    public int getInt(String key, Integer defVal) {
        if (defVal == null) {
            return Integer.parseInt(properties.getProperty(key));
        }
        return Integer.parseInt(properties.getProperty(key, defVal + ""));
    }
}
