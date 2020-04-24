package ee.ut.jbizur.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A utility class that is used as an executor but used to report any ee.ut.jbizur.exceptions caught when {@link #execute(Runnable)}
 * method is run.
 */
public class MultiThreadExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutor.class);

    /**
     * Executes the {@link Runnable} passed to the {@link #execute(Runnable)} method.
     */
    private final ExecutorService executor;
    /**
     * Total executions required.
     */
    private final List<Future<?>> futures = new ArrayList<>();

    private final String execId = TestUtil.getRandomString();

    // enables overlapping of threads.
    private final CountDownLatch latch = new CountDownLatch(1);

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
        futures.add(executor.submit(() -> {
            try {
                latch.await();
                runnable.run();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }));
    }

    /**
     * Waits for all the executions to complete.
     * @throws InterruptedException if latch await fails.
     */
    public void endExecution() throws InterruptedException, ExecutionException {
        logger.info("ending execution id=" + execId);
        executor.shutdown();
        latch.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        logger.info("execution ended id=" + execId);
    }
}
