package ee.ut.jbizur.datastore.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.util.IdUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BucketContainer {

    private final Map<Integer, Bucket> localBuckets;
    private final int numBuckets;

    private final Map<Address, Set<Integer>> addressBucketIndexMap = new HashMap<>();

    public BucketContainer(int numBuckets) {
        this.numBuckets = numBuckets;
        this.localBuckets = new ConcurrentHashMap<>();
    }

    public Bucket getBucket(String key) {
        return getBucket(hashKey(key));
    }

    public Bucket getBucket(int index) {
        return localBuckets.computeIfAbsent(index, idx -> new Bucket(this).setIndex(idx));
    }

    public void lockBucket(int index) {
        getBucket(index).lock();
    }

    public void unlockBucket(int index) {
        getBucket(index).unlock();
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
                .filter(bucket -> bucket.getLeaderAddress().equals(address))
                .map(Bucket::getIndex)
                .collect(Collectors.toSet());
    }

    public int hashKey(String s) {
        return IdUtils.hashKey(s, numBuckets);
    }
}
