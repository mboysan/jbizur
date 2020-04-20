package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.net.BucketView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bucket implements Comparable<Bucket> {
    private static final Logger logger = LoggerFactory.getLogger(Bucket.class);

    private final Lock bucketLock = new ReentrantLock();
    private final Lock apiLock = new ReentrantLock();
    private final ReadWriteLock mapLock = new ReentrantReadWriteLock();

    private final AtomicReference<Address> leaderAddress = new AtomicReference<>(null);
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicInteger electId = new AtomicInteger(0);
    private final AtomicInteger votedElectId = new AtomicInteger(0);

    /**
     * We don't need to create a ConcHashMap because we will be protected by a lock.
     */
    private final Map<String,String> bucketMap = new HashMap<>();
    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicInteger verElectId = new AtomicInteger(0);
    private final AtomicInteger verCounter = new AtomicInteger(0);

    Bucket() {}

    /* ***************************************************************************
     * Map Operations
     * ***************************************************************************/

    public String putOp(String key, String val){
        mapLock.writeLock().lock();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("put key={},val={} in bucket={}", key, val, this);
            }
            return bucketMap.put(key, val);
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    public String getOp(String key){
        mapLock.readLock().lock();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("get key={} from bucket={}", key, this);
            }
            return bucketMap.get(key);
        } finally {
            mapLock.readLock().unlock();
        }
    }

    public String removeOp(String key){
        mapLock.writeLock().lock();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("remove key={} from bucket={}", key, this);
            }
            return bucketMap.remove(key);
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    public Set<String> getKeySetOp(){
        mapLock.readLock().lock();
        try {
            return bucketMap.keySet();
        } finally {
            mapLock.readLock().unlock();
        }
    }

    /* ***************************************************************************
     * Specifications
     * ***************************************************************************/

    public Bucket setBucketMap(Map map) {
        mapLock.writeLock().lock();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("replacing bucketMap={} with map={} in bucket={}", bucketMap, map, this);
            }
            this.bucketMap.clear();
            this.bucketMap.putAll(map);
            return this;
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    public int getIndex() {
        return index.get();
    }

    public Bucket setIndex(int index) {
        this.index.set(index);
        return this;
    }

    public Bucket setLeaderAddress(Address leaderAddress) {
        this.leaderAddress.set(leaderAddress);
        return this;
    }

    public Address getLeaderAddress() {
        return leaderAddress.get();
    }

    public Bucket setLeader(boolean isLeader) {
        this.isLeader.set(isLeader);
        return this;
    }
    public boolean isLeader() {
        return isLeader.get();
    }

    public int getElectId() {
        return electId.get();
    }
    public Bucket setElectId(int electId) {
        this.electId.set(electId);
        return this;
    }
    public int incrementAndGetElectId() {
        return this.electId.incrementAndGet();
    }

    public Bucket setVotedElectId(int votedElectId) {
        this.votedElectId.set(votedElectId);
        return this;
    }
    public int getVotedElectId() {
        return votedElectId.get();
    }

    public int getVerElectId() {
        return verElectId.get();
    }
    public Bucket setVerElectId(int verElectId) {
        this.verElectId.set(verElectId);
        return this;
    }

    public int incrementAndGetVerCounter() {
        return this.verCounter.incrementAndGet();
    }
    public Bucket setVerCounter(int verCounter) {
        this.verCounter.set(verCounter);
        return this;
    }
    public int getVerCounter() {
        return verCounter.get();
    }


    /* ***************************************************************************
     * BucketView
     * ***************************************************************************/

    public BucketView createView() {
        mapLock.readLock().lock();
        try {
            return new BucketView()
                    .setBucketMap(new HashMap<>(bucketMap))
                    .setIndex(getIndex())
                    .setVerElectId(getVerElectId())
                    .setVerCounter(getVerCounter())
                    .setLeaderAddress(getLeaderAddress());
        } finally {
            mapLock.readLock().unlock();
        }
    }

    public static Bucket createBucket(BucketView bucketView) {
        return new Bucket()
                .setIndex(bucketView.getIndex())
                .setBucketMap(bucketView.getBucketMap())
                .setVerElectId(bucketView.getVerElectId())
                .setVerCounter(bucketView.getVerCounter())
                .setLeaderAddress(bucketView.getLeaderAddress());
    }

    public int compareToView(BucketView bucketView) {
        if(this.getVerElectId() > bucketView.getVerElectId()){
            return 1;
        } else if (this.getVerElectId() == bucketView.getVerElectId()){
            return Integer.compare(this.getVerCounter(), bucketView.getVerCounter());
        } else {
            return -1;
        }
    }


    /* ***************************************************************************
     * Utils
     * ***************************************************************************/

    @Override
    public int compareTo(Bucket o) {
        if(this.getVerElectId() > o.getVerElectId()){
            return 1;
        } else if (this.getVerElectId() == o.getVerElectId()){
            return Integer.compare(this.getVerCounter(), o.getVerCounter());
        } else {
            return -1;
        }
    }

    public boolean isLocked() {
        if (bucketLock.tryLock()) {
            try {
                return false;
            } finally {
                bucketLock.unlock();
            }
        }
        return true;
    }

    public void lock() {
        bucketLock.lock();
    }

    public void unlock() {
        bucketLock.unlock();
    }

    public void apiLock() {
        apiLock.lock();
    }

    public void apiUnlock() {
        apiLock.unlock();
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "leaderAddress=" + leaderAddress +
                ", isLeader=" + isLeader +
                ", electId=" + electId +
                ", votedElectId=" + votedElectId +
                ", index=" + index +
                ", verElectId=" + verElectId +
                ", verCounter=" + verCounter +
                '}';
    }
}