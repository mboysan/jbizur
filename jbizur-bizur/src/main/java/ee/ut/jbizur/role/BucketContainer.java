package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.config.CoreConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BucketContainer {

    private static final Logger logger = LoggerFactory.getLogger(BucketContainer.class);

    private final Map<Integer, Bucket> localBuckets;
    private final int numBuckets;
    private final String name;

    public BucketContainer(String name, int numBuckets) {
        this.name = name;
        this.numBuckets = numBuckets;
        this.localBuckets = new ConcurrentHashMap<>();
    }

    private Bucket getOrCreateBucket(int index) {
        return localBuckets.computeIfAbsent(index, idx -> new Bucket().setIndex(idx));
    }

    Bucket tryAndLockBucket(int index) {
        return tryAndLockBucket(index, -1);
    }

    Bucket tryAndLockBucket(int index, int contextId) {
        Bucket bucket = getOrCreateBucket(index);
        try {
            long bucketLockTimeoutMs = CoreConf.get().consensus.bizur.bucketLockTimeoutMs;
            if (bucketLockTimeoutMs >= 0) {
                if (bucket.tryLock(bucketLockTimeoutMs, TimeUnit.MILLISECONDS)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[{}] locked bucket (1), contextId={}, bucket={}", name, contextId, getOrCreateBucket(index));
                    }
                    return bucket;
                }
            } else {
                bucket.lock();
                if (logger.isDebugEnabled()) {
                    logger.debug("[{}] locked bucket (2), contextId={}, bucket={}", name, contextId, getOrCreateBucket(index));
                }
                return bucket;
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] bucket already locked, contextId={}, bucket={}", name, contextId, getOrCreateBucket(index));
        }
        return null;
    }

    Set<Integer> collectIndices() {
        return localBuckets.keySet();
    }

    int hashKey(String s) {
        return IdUtil.hashKey(s, numBuckets);
    }

    @Override
    public String toString() {
        return "BucketContainer{" +
                "localBuckets=" + localBuckets +
                ", numBuckets=" + numBuckets +
                ", name='" + name + '\'' +
                '}';
    }
}
