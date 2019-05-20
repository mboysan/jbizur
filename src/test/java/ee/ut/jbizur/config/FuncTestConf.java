package ee.ut.jbizur.config;

import java.io.File;

public class FuncTestConf extends Conf {
    static {
        setConfig(new File(Conf.class.getClassLoader().getResource("jbizur_func_test.conf").getFile()));
        LogConf.configureLogger();
    }

    public static JbizurConfig get() {
        return Conf.get();
    }
}
