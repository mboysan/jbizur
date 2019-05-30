package ee.ut.jbizur.config;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.FileWriter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Configures the logging interface
 */
public class LogConf {

    public static synchronized void configureLogger(){
        String levelStr = Conf.get().logging.level;
        String pattern = Conf.get().logging.pattern;
        String file = Conf.get().logging.writeToFile;
        boolean console = Conf.get().logging.writeToConsole;

        Configurator c = Configurator.currentConfig()
                .formatPattern(pattern)
                .level(Level.valueOf(levelStr));
        if (file != null) {
            try {
                String fw = String.format(file, getCurrentTimeStamp());
                c.writer(new FileWriter(fw)).writingThread("main", Thread.MIN_PRIORITY);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        if (console) {
            c.addWriter(new ConsoleWriter());
        }
        c.activate();
    }

    public static boolean isDebugEnabled() {
        return Logger.getLevel().equals(Level.DEBUG);
    }

    private static String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());//dd/MM/yyyy
    }
}
