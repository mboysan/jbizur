package ee.ut.jbizur.config;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

/**
 * Configures the logging interface
 */
public class LoggerConfig {
    public static void configureLogger(){
        configureLogger(Level.DEBUG);
//        configureLogger(Level.INFO);
//        configureLogger(Level.ERROR);
    }

    public static void configureLogger(Level level){
        configRefined(level);
    }

    /**
     * For a more refined output.
     * @param level log level
     */
    private static void configRefined(Level level){
        Configurator.currentConfig()
                .formatPattern("[{level}] {date:HH:mm:ss:SSS} {class}.{method}(): {message}")
                .level(level)
                .activate();
    }

    /**
     * For a more detailed output.
     * @param level log level
     */
    private static void configDetailed(Level level){
        Configurator.currentConfig()
                .formatPattern("{date:HH:mm:ss:SSS} [{level}] {thread}-{class}.{method}(): {message}")
                .level(level)
                .activate();
    }
}
