package ee.ut.jbizur.datastore.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.ISerializer;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BucketContainer {

    private final Bucket[] localBuckets;
    private final int numBuckets;

    private final Map<String, Set<Integer>> addressBucketIndexMap = new HashMap<>();
    private final ISerializer serializer = new ByteSerializer();

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
            String newAddrStr = serializer.serializeToString(newAddr);
            addressBucketIndexMap.putIfAbsent(newAddrStr, new HashSet<>());

            Address prevAddr = getBucket(bucketIndex).getLeaderAddress();
            if (prevAddr != null) {
                if (prevAddr.isSame(newAddr)) {
                    return;
                }
                String prevAddrStr = serializer.serializeToString(prevAddr);
                addressBucketIndexMap.get(prevAddrStr).remove(bucketIndex);
            }
            addressBucketIndexMap.get(newAddrStr).add(bucketIndex);
        }
    }

    public int getNumBuckets() {
        return numBuckets;
    }

    public Set<Address> collectAddressesWithBucketLeaders() {
        return addressBucketIndexMap.keySet().stream()
                .map(s -> (Address) serializer.deSerializeFromString(s))
                .collect(Collectors.toSet());
    }

    public Set<Integer> bucketIndices(Address address) {
        return addressBucketIndexMap.get(serializer.serializeToString(address));
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
