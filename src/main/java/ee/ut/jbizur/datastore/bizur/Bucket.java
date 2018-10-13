package ee.ut.jbizur.datastore.bizur;

import ee.ut.jbizur.config.LoggerConfig;
import ee.ut.jbizur.network.address.Address;
import org.pmw.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Bucket {

    private final BucketLock bucketLock = new BucketLock();

    private final AtomicReference<Address> leaderAddress;
    private final AtomicBoolean isLeader;
    private final AtomicInteger electId;
    private final AtomicInteger votedElectId;

    private final Map<String,String> bucketMap;
    private final AtomicInteger index;
    private final AtomicInteger verElectId;
    private final AtomicInteger verCounter;

    private final BucketContainer bucketContainer;

    private AtomicBoolean electionInProgress;

    Bucket(BucketContainer bucketContainer) {
        bucketMap = new ConcurrentHashMap<>();
        index = new AtomicInteger(0);
        leaderAddress = new AtomicReference<>(null);
        isLeader = new AtomicBoolean(false);

        int initId = 0;
        electId = new AtomicInteger(initId);
        votedElectId = new AtomicInteger(initId);

        verElectId = new AtomicInteger(initId);
        verCounter = new AtomicInteger(0);

        this.bucketContainer = bucketContainer;

        this.electionInProgress = new AtomicBoolean(false);
    }

    public String putOp(String key, String val){
        if (LoggerConfig.isDebugEnabled()) {
            Logger.debug(String.format("put key=[%s],val=[%s] in bucket=[%s]", key, val, this));
        }
        return bucketMap.put(key, val);
    }

    public String getOp(String key){
        return bucketMap.get(key);
    }

    public String removeOp(String key){
        return bucketMap.remove(key);
    }

    public Set<String> getKeySet(){
        return bucketMap.keySet();
    }

    public int getIndex() {
        return index.get();
    }

    public Bucket setBucketMap(Map map) {
        if (bucketMap.size() > 0 && map.size() == 0) {
            if (LoggerConfig.isDebugEnabled()) {
                Logger.debug(String.format("removing from bucket=[%s] and inserting elements from map=[%s] in bucket=[%s]", bucketMap, map, this));
            }
        }
        this.bucketMap.clear();
        this.bucketMap.putAll(map);
        return this;
    }

    public Bucket setIndex(int index) {
        this.index.set(index);
        return this;
    }

    public Bucket setLeaderAddress(Address leaderAddress) {
        return setLeaderAddress(leaderAddress, true);
    }

    public Bucket setLeaderAddress(Address leaderAddress, boolean update) {
        if (update) {
            bucketContainer.updateLeaderAddress(getIndex(), leaderAddress);
        }
        this.leaderAddress.set(leaderAddress);
        return this;
    }

    public Address getLeaderAddress() {
        return leaderAddress.get();
    }

    public Bucket updateLeader(boolean isLeader) {
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

    public Bucket setElectionInProgress(boolean electionInProgress) {
        this.electionInProgress.set(electionInProgress);
        return this;
    }
    public boolean isElectionInProgress() {
        return electionInProgress.get();
    }

    public BucketView createView(){
        return new BucketView()
                .setBucketMap(new HashMap<>(bucketMap))
                .setIndex(getIndex())
                .setVerElectId(getVerElectId())
                .setVerCounter(getVerCounter())
                .setLeaderAddress(getLeaderAddress());
    }

    public Bucket replaceBucketForReplicationWith(Bucket bucket){
        return replaceBucketForReplicationWith(bucket.createView());
    }

    public Bucket replaceBucketForReplicationWith(BucketView bucketView) {
        return setBucketMap(bucketView.getBucketMap())
//                    .setIndex(bucketView.getIndex())
//                    .setVerElectId(bucketView.getVerElectId())
//                    .setVerCounter(bucketView.getVerCounter())
                .setLeaderAddress(bucketView.getLeaderAddress(), false)
                .setVotedElectId(bucketView.getVerElectId());
    }

    public int compareVersion(Bucket other) {
        return compareVersions(this, other);
    }

    public static int compareVersions(Bucket mainBucket, Bucket otherBucket) {
        if(mainBucket.getVerElectId() > otherBucket.getVerElectId()){
            return 1;
        } else if (mainBucket.getVerElectId() == otherBucket.getVerElectId()){
            return Integer.compare(mainBucket.getVerCounter(), otherBucket.getVerCounter());
        } else {
            return -1;
        }
    }

    public void lock() {
        try {
            bucketLock.lock();
        } catch (InterruptedException e) {
            Logger.error(e);
        }
    }

    public void unlock() {
        bucketLock.unlock();
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
