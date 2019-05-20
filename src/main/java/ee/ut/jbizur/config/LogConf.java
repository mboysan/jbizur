package ee.ut.jbizur.config;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
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
        String file = Conf.get().logging.file;

        Configurator c = Configurator.currentConfig()
                .formatPattern(pattern)
                .level(Level.valueOf(levelStr));
        if (file != null) {
            try {
                String fw = String.format(file, getCurrentTimeStamp());
                c.writer(new FileWriter(fw));
            } catch (Exception e) {}
        }
        c.activate();
    }

    public static boolean isDebugEnabled() {
        return Logger.getLevel().equals(Level.DEBUG);
    }

    private static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd_HHmmss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
}
