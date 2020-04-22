package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.protocol.address.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BucketContainer {

    private static final Logger logger = LoggerFactory.getLogger(BucketContainer.class);

    private final Map<Integer, Bucket> localBuckets;
    private final int numBuckets;

    public BucketContainer(int numBuckets) {
        this.numBuckets = numBuckets;
        this.localBuckets = new ConcurrentHashMap<>();
    }

    private Bucket getOrCreateBucket(int index) {
        return localBuckets.computeIfAbsent(index, idx -> new Bucket().setIndex(idx));
    }

    Bucket lockAndGetBucket(int index) {
        Bucket bucket = getOrCreateBucket(index);
        bucket.lock();
        return bucket;
    }

    Bucket tryAndLockBucket(int index) {
        Bucket bucket = getOrCreateBucket(index);
        try {
            if (bucket.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                return bucket;
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("bucket is locked, bucket=" + getOrCreateBucket(index));
        }
        return null;
    }

    void unlockBucket(int index) {
        getOrCreateBucket(index).unlock();
    }

    void apiLock(int index) {
        getOrCreateBucket(index).apiLock();
    }

    void apiUnlock(int index) {
        getOrCreateBucket(index).apiUnlock();
    }

    int getNumBuckets() {
        return numBuckets;
    }

    Set<Address> collectAddressesWithBucketLeaders() {
        return localBuckets.values().stream()
                .map(Bucket::getLeaderAddress)
                .collect(Collectors.toSet());
    }

    Set<Integer> bucketIndicesOfAddress(Address address) {
        return localBuckets.values().stream()
                .filter(bucket -> bucket.getLeaderAddress() != null && bucket.getLeaderAddress().equals(address))
                .map(Bucket::getIndex)
                .collect(Collectors.toSet());
    }

    Set<Integer> collectIndices() {
        return localBuckets.keySet();
    }

    int hashKey(String s) {
        return IdUtil.hashKey(s, numBuckets);
    }
}
