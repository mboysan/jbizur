package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * A synchronous listener that waits for responses through a {@link #latch}.
 */
public abstract class AbstractSyncedListener implements Predicate<NetworkCommand> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncedListener.class);

    private final CountDownLatch latch;
    private final long timeoutMs;

    public AbstractSyncedListener(int totalSize, long timeoutMillis) {
        this.latch = new CountDownLatch(totalSize);
        this.timeoutMs = timeoutMillis;
    }

    void countdown() {
        latch.countDown();
    }

    void terminate() {
        while (latch.getCount() > 0) {
            countdown();
        }
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
