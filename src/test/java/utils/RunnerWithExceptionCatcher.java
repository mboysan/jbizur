package utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility class that is used as an executor but used to report any exceptions caught when {@link #execute(Runnable)}
 * method is run.
 */
public class RunnerWithExceptionCatcher {

    /**
     * Executes the {@link Runnable} passed to the {@link #execute(Runnable)} method.
     */
    private final ExecutorService executor;
    /**
     * Any exception caught when {@link Runnable#run()} method is executed inside the {@link #execute(Runnable)}
     * method.
     */
    private final AtomicReference<Throwable> caughtException;
    /**
     * Total executions required.
     */
    private final CountDownLatch latch;

    public RunnerWithExceptionCatcher(int latchCount) {
        this(latchCount, Runtime.getRuntime().availableProcessors());
    }

    public RunnerWithExceptionCatcher(int latchCount, int threadCount) {
        caughtException = new AtomicReference<>(null);
        latch = new CountDownLatch(latchCount);
        executor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * Runs the <tt>runnable</tt> passed with the {@link #executor}. Catches any exceptions ({@link Throwable})
     * caught during the execution of the runnable's {@link Runnable#run()} method.
     *
     * @param runnable the runnable to execute and report any exceptions caught when running it.
     */
    public void execute(Runnable runnable) {
        executor.execute(() -> {
            try {
                if (caughtException.get() != null) {
                    return;
                }
                runnable.run();
            } catch (Throwable e) {
                caughtException.compareAndSet(null, e);
            } finally {
                latch.countDown();
            }
        });
    }

    /**
     * Throws the caught exception if any. Otherwise does nothing.
     * @throws Throwable if any exception is caught during the execution of {@link #execute(Runnable)} method.
     */
    public void throwAnyCaughtException() throws Throwable {
        if (caughtException.get() != null) {
            throw caughtException.get();
        }
    }

    /**
     * Waits for all the executions to complete.
     * @throws InterruptedException if latch await fails.
     */
    public void awaitCompletion() throws InterruptedException {
        latch.await();
    }
}
