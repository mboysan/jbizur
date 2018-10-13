package ee.ut.bench.config;

import java.io.File;

public abstract class Config {
    protected static PropsLoader _loadPropertiesFromResources(String fileName) {
        return PropsLoader.loadProperties(Config.class, fileName);
    }
    public static PropsLoader _loadPropertiesFromWorkingDir(String fileName) {
        File relFile = new File((new File("user.dir")).toURI().relativize(new File(fileName).toURI()).getPath());
        return PropsLoader.loadProperties(relFile);
    }

}
