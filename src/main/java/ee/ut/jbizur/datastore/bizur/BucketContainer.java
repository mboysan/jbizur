package ee.ut.jbizur.datastore.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BucketContainer {

    private final Bucket[] localBuckets;
    private final int numBuckets;

    private final Map<Address, Set<Integer>> addressBucketIndexMap = new HashMap<>();

    public BucketContainer(int numBuckets) {
        this.numBuckets = numBuckets;
        this.localBuckets = new Bucket[numBuckets];
    }

    public BucketContainer initBuckets() {
        for (int i = 0; i < localBuckets.length; i++) {
            localBuckets[i] = new Bucket(this).setIndex(i);
        }
        Logger.info("buckets are initialized!");
        return this;
    }

    public Bucket getBucket(String key) {
        return getBucket(hashKey(key));
    }

    public Bucket getBucket(int index) {
        return localBuckets[index];
    }

    public void lockBucket(int index) {
        getBucket(index).lock();
    }

    public void unlockBucket(int index) {
        getBucket(index).unlock();
    }

    public void updateLeaderAddress(int bucketIndex, Address newAddr) {
        if (newAddr == null) {
            throw new IllegalStateException("new address must not be null!");
        }
        synchronized (addressBucketIndexMap) {
            addressBucketIndexMap.putIfAbsent(newAddr, new HashSet<>());

            Address prevAddr = getBucket(bucketIndex).getLeaderAddress();
            if (prevAddr != null) {
                if (prevAddr.equals(newAddr)) {
                    return;
                }
                addressBucketIndexMap.get(prevAddr).remove(bucketIndex);
            }
            addressBucketIndexMap.get(newAddr).add(bucketIndex);
        }
    }

    public int getNumBuckets() {
        return numBuckets;
    }

    public Set<Address> collectAddressesWithBucketLeaders() {
        return addressBucketIndexMap.keySet();
    }

    public Set<Integer> bucketIndices(Address address) {
        return addressBucketIndexMap.get(address);
    }

    /**
     * Taken from <a href="https://algs4.cs.princeton.edu/34hash/">34hash site</a>.
     * @param s key to hash.
     * @return index of the bucket.
     */
    public int hashKey(String s) {
        return IdUtils.hashKey(s, numBuckets);
    }
}
