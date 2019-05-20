package ee.ut.jbizur.config;

import java.io.File;

public class IntegTestConf extends Conf {
    static {
        setConfig(new File(Conf.class.getClassLoader().getResource("jbizur_integ_test.conf").getFile()));
        LogConf.configureLogger();
    }

    public static JbizurConfig get() {
        return Conf.get();
    }
}
