package ee.ut.jbizur.config;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

/**
 * Configures the logging interface
 */
public class LoggerConfig {

    public static void configureLogger(){
        String levelStr = PropertiesLoader.getString("logger.level");
        String pattern = PropertiesLoader.getString("logger.pattern");

        Configurator.currentConfig()
                .formatPattern(pattern)
                .level(Level.valueOf(levelStr))
                .activate();
    }
}
