package ee.ut.jbizur.config;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

/**
 * Configures the logging interface
 */
public class LoggerConfig {

    public static void configureLogger(){
        String levelStr = PropertiesLoader.getString("logger.level", "INFO");
        String pattern = PropertiesLoader.getString("logger.pattern");

        Configurator.currentConfig()
                .formatPattern(pattern)
                .level(Level.valueOf(levelStr))
                .activate();
    }

    public static boolean isDebugEnabled() {
        return Logger.getLevel().equals(Level.DEBUG);
    }
}
