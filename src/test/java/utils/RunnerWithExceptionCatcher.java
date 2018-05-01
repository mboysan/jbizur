package utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class RunnerWithExceptionCatcher {

    private final ExecutorService executor;
    private final AtomicReference<Throwable> caughtException;
    private final CountDownLatch latch;

    public RunnerWithExceptionCatcher(int latchCount, int threadCount) {
        caughtException = new AtomicReference<>(null);
        latch = new CountDownLatch(latchCount);
        if (threadCount <= 0) {
            executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        } else {
            executor = Executors.newFixedThreadPool(threadCount);
        }
    }

    public void execute(Runnable runnable) {
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
    }

    public void throwCaughtException() throws Throwable {
        if (caughtException.get() != null) {
            throw caughtException.get();
        }
    }

    public void awaitCompletion() throws InterruptedException {
        latch.await();
    }
}
