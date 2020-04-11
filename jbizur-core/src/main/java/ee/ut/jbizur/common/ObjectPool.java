package ee.ut.jbizur.common;

// ObjectPool Class

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Inspired by <a href="https://sourcemaking.com/design_patterns/object_pool/java">sourcemaking: ObjectPool</a>
 * @param <T>
 */
public abstract class ObjectPool<T extends AutoCloseable> implements AutoCloseable, ResourceCloser {

    private static final Logger logger = LoggerFactory.getLogger(ObjectPool.class);

    private static final long DEFAULT_EXPIRATION_MS = 30000;    // 30 seconds

    private final long expirationTime;

    private transient boolean isRunning;

    private Map<T, Long> locked, unlocked;

    public ObjectPool() {
        this(DEFAULT_EXPIRATION_MS);
    }

    public ObjectPool(long expirationTime) {
        this.expirationTime = expirationTime;
        locked = new HashMap<>();
        unlocked = new HashMap<>();
        isRunning = true;
    }

    protected abstract T create();

    protected abstract boolean validate(T o);

    protected abstract void expire(T o);

    public synchronized T checkOut() {
        validateAction();
        long now = System.currentTimeMillis();
        for (T t : unlocked.keySet()) {
            boolean isExpired = expirationTime > 0 // check expiration only if exptime > 0
                    && (now - unlocked.get(t)) > expirationTime;
            if (isExpired || !validate(t)) {
                // object has expired or validation failed
                unlocked.remove(t);
                expire(t);
            } else {
                return t;
            }
        }
        // no objects available, create a new one
        T t = create();
        locked.put(t, now);
        return (t);
    }

    public synchronized void checkIn(T t) {
        validateAction();
        locked.remove(t);
        unlocked.put(t, System.currentTimeMillis());
    }

    protected void validateAction() {
        if (!isRunning) {
            throw new IllegalStateException("pool closed");
        }
    }

    @Override
    public synchronized void close() {
        isRunning = false;
        closeResources(logger, locked.keySet());
        closeResources(logger, unlocked.keySet());
    }
}