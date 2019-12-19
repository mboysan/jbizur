package ee.ut.jbizur.util;

public final class LambdaUtils {

    private LambdaUtils(){}

    public static <E extends Exception> Runnable runnable(ThrowingRunnable<E> r) {
        return () -> {
            try {
                r.run();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @FunctionalInterface
    public interface ThrowingRunnable <E extends Exception> {
        void run() throws E;
    }

}
