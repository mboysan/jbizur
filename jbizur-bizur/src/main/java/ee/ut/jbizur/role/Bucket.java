package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.net.BucketView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Bucket<K, V> implements Comparable<Bucket<K, V>> {
    private static final Logger logger = LoggerFactory.getLogger(Bucket.class);

    private final ReentrantLock bucketLock = new ReentrantLock();

    private final int index;

    private Address leaderAddress = null;
    private boolean isLeader = false;
    private int electId = 0;
    private int votedElectId = 0;

    private final Map<K, V> bucketMap = new HashMap<>();
    private int verElectId = 0;
    private int verCounter = 0;

    Bucket(int index) {
        this.index = index;
    }

    /* ***************************************************************************
     * Map Operations
     * ***************************************************************************/

    public V putOp(K key, V val) {
        if (logger.isDebugEnabled()) {
            logger.debug("put key={},val={} in bucket={}", key, val, this);
        }
        return bucketMap.put(key, val);
    }

    public V getOp(K key) {
        if (logger.isDebugEnabled()) {
            logger.debug("get key={} from bucket={}", key, this);
        }
        return bucketMap.get(key);
    }

    public V removeOp(K key) {
        if (logger.isDebugEnabled()) {
            logger.debug("remove key={} from bucket={}", key, this);
        }
        return bucketMap.remove(key);
    }

    public Set<K> getKeySetOp() {
        return bucketMap.keySet();
    }

    /* ***************************************************************************
     * Specifications
     * ***************************************************************************/

    public Bucket<K, V> setBucketMap(Map map) {
        if (logger.isDebugEnabled()) {
            logger.debug("replacing bucketMap={} with map={} in bucket={}", bucketMap, map, this);
        }
        this.bucketMap.clear();
        this.bucketMap.putAll(map);
        return this;
    }

    public int getIndex() {
        return index;
    }

    public Bucket<K, V> setLeaderAddress(Address leaderAddress) {
        this.leaderAddress = leaderAddress;
        return this;
    }

    public Address getLeaderAddress() {
        return leaderAddress;
    }

    public Bucket<K, V> setLeader(boolean isLeader) {
        this.isLeader = isLeader;
        return this;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public int getElectId() {
        return electId;
    }

    public Bucket<K, V> setElectId(int electId) {
        this.electId = electId;
        return this;
    }

    public int incrementAndGetElectId() {
        this.electId++;
        return this.electId;
    }

    public Bucket setVotedElectId(int votedElectId) {
        this.votedElectId = votedElectId;
        return this;
    }

    public int getVotedElectId() {
        return votedElectId;
    }

    public int getVerElectId() {
        return verElectId;
    }

    public Bucket<K, V> setVerElectId(int verElectId) {
        this.verElectId = verElectId;
        return this;
    }

    public int incrementAndGetVerCounter() {
        this.verCounter++;
        return this.verCounter;
    }

    public Bucket<K, V> setVerCounter(int verCounter) {
        this.verCounter = verCounter;
        return this;
    }

    public int getVerCounter() {
        return verCounter;
    }


    /* ***************************************************************************
     * BucketView
     * ***************************************************************************/

    public BucketView<K, V> createView() {
        return new BucketView<K, V>()
                .setBucketMap(new HashMap<>(bucketMap))
                .setIndex(getIndex())
                .setVerElectId(getVerElectId())
                .setVerCounter(getVerCounter())
                .setLeaderAddress(getLeaderAddress());
    }

    /* ***************************************************************************
     * Utils
     * ***************************************************************************/

    public int compareToView(BucketView<K, V> bucketView) {
        if (this.getVerElectId() > bucketView.getVerElectId()) {
            return 1;
        } else if (this.getVerElectId() == bucketView.getVerElectId()) {
            return Integer.compare(this.getVerCounter(), bucketView.getVerCounter());
        } else {
            return -1;
        }
    }

    @Override
    public int compareTo(Bucket o) {
        if (this.getVerElectId() > o.getVerElectId()) {
            return 1;
        } else if (this.getVerElectId() == o.getVerElectId()) {
            return Integer.compare(this.getVerCounter(), o.getVerCounter());
        } else {
            return -1;
        }
    }

    boolean isLocked() {
        return bucketLock.isLocked();
    }

    boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return bucketLock.tryLock(timeout, timeUnit);
    }

    void lock() {
        bucketLock.lock();
    }

    void unlock() {
        bucketLock.unlock();
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "index=" + index +
                ", leaderAddress=" + leaderAddress +
                ", isLeader=" + isLeader +
                ", electId=" + electId +
                ", votedElectId=" + votedElectId +
                ", verElectId=" + verElectId +
                ", verCounter=" + verCounter +
                '}';
    }
}
