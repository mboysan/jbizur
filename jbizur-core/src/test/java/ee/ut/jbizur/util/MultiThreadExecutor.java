package ee.ut.jbizur.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A utility class that is used as an executor but used to report any ee.ut.jbizur.exceptions caught when {@link #execute(Runnable)}
 * method is run.
 */
public class MultiThreadExecutor {

    /**
     * Executes the {@link Runnable} passed to the {@link #execute(Runnable)} method.
     */
    private final ExecutorService executor;
    /**
     * Total executions required.
     */
    private final List<Future<?>> futures = new ArrayList<>();

    public MultiThreadExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public MultiThreadExecutor(int threadCount) {
        executor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * Runs the <tt>runnable</tt> passed with the {@link #executor}. Catches any ee.ut.jbizur.exceptions ({@link Throwable})
     * caught during the execution of the runnable's {@link Runnable#run()} method.
     *
     * @param runnable the runnable to handle and report any ee.ut.jbizur.exceptions caught when running it.
     */
    public void execute(Runnable runnable) {
        futures.add(executor.submit(runnable));
    }

    /**
     * Waits for all the executions to complete.
     * @throws InterruptedException if latch await fails.
     */
    public void endExecution() throws InterruptedException, ExecutionException {
        executor.shutdown();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
