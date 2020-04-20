package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.protocol.address.Address;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BucketContainer {

    private final Map<Integer, Bucket> localBuckets;
    private final int numBuckets;

    public BucketContainer(int numBuckets) {
        this.numBuckets = numBuckets;
        this.localBuckets = new ConcurrentHashMap<>();
    }

    private Bucket getOrCreateBucket(int index) {
        return localBuckets.computeIfAbsent(index, idx -> new Bucket().setIndex(idx));
    }

    public Bucket lockAndGetBucket(int index) {
        Bucket bucket = getOrCreateBucket(index);
        bucket.lock();
        return bucket;
    }

    public void unlockBucket(int index) {
        getOrCreateBucket(index).unlock();
    }

    public void apiLock(int index) {
        getOrCreateBucket(index).apiLock();
    }

    public void apiUnlock(int index) {
        getOrCreateBucket(index).apiUnlock();
    }

    public int getNumBuckets() {
        return numBuckets;
    }

    public Set<Address> collectAddressesWithBucketLeaders() {
        return localBuckets.values().stream()
                .map(Bucket::getLeaderAddress)
                .collect(Collectors.toSet());
    }

    public Set<Integer> bucketIndicesOfAddress(Address address) {
        return localBuckets.values().stream()
                .filter(bucket -> bucket.getLeaderAddress() != null && bucket.getLeaderAddress().equals(address))
                .map(Bucket::getIndex)
                .collect(Collectors.toSet());
    }

    Set<Integer> collectIndices() {
        return localBuckets.keySet();
    }

    public int hashKey(String s) {
        return IdUtil.hashKey(s, numBuckets);
    }
}
