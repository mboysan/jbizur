package ee.ut.jbizur.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

abstract class CountdownLambda {
    private static final Logger logger = LoggerFactory.getLogger(CountdownConsumer.class);

    private final CountDownLatch latch;
    private final long timeoutMs;


    CountdownLambda(int count) {
        this(count, 0);
    }

    CountdownLambda(int count, long timeoutMillis) {
        this.latch = new CountDownLatch(count);
        this.timeoutMs = timeoutMillis;
    }

    protected void terminate() {
        while (latch.getCount() > 0) {
            countdown();
        }
    }

    protected void countdown() {
        latch.countDown();
    }

    public boolean await() {
        return await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean await(long timeout, TimeUnit timeUnit) {
        try {
            if (timeout > 0) {
                return latch.await(timeout, timeUnit);
            }
            latch.await();
            return true;
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
