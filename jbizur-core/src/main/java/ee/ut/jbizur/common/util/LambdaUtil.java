package ee.ut.jbizur.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LambdaUtil {

    private static final Logger logger = LoggerFactory.getLogger(LambdaUtil.class);

    private LambdaUtil(){}

    public static <E extends Exception> Runnable runnable(ThrowingRunnable<E> r) {
        return () -> {
            try {
                r.run();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }

    @FunctionalInterface
    public interface ThrowingRunnable <E extends Exception> {
        void run() throws E;
    }

}
