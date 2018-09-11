package ee.ut.jbizur.datastore.bizur;

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

    private final Object mapLock = new Object();
    private Map<String,String> bucketMap = new ConcurrentHashMap<>();

    private final AtomicReference<Ver> ver = new AtomicReference<>(new Ver());
    private final AtomicInteger index = new AtomicInteger(0);
    private AtomicReference<Address> leaderAddress = new AtomicReference<>(null);
    private AtomicBoolean isLeader = new AtomicBoolean(false);

    public String putOp(String key, String val){
        synchronized (mapLock) {
            return bucketMap.put(key, val);
        }
    }

    public String getOp(String key){
        synchronized (mapLock) {
            return bucketMap.get(key);
        }
    }

    public String removeOp(String key){
        synchronized (mapLock) {
            return bucketMap.remove(key);
        }
    }

    public Set<String> getKeySet(){
        synchronized (mapLock) {
            return bucketMap.keySet();
        }
    }

    public Ver getVer() {
        return ver.get();
    }

    public int getIndex() {
        return index.get();
    }

    public Bucket setIndex(int index) {
        this.index.set(index);
        return this;
    }

    public Bucket setLeaderAddress(Address leaderAddress) {
        /*
        if (isLeader() && !getLeaderAddress().isSame(leaderAddress)) {
            updateLeader(false);
        }
        */
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

    public void replaceWithBucket(Bucket bucket){
        replaceWithView(bucket.createView());
//        synchronized (mapLock) {
//            bucketMap = new ConcurrentHashMap<>(bucket.bucketMap);
//            index.set(bucket.getIndex());
//            ver.set(bucket.getVer());
//        }
        /*
            bucketMap.keySet().forEach(s -> {
                if(!bucket.bucketMap.contains(s)){
                    bucketMap.remove(s);
                }
            });
            */
//            bucketMap.putAll(bucket.bucketMap);
//        bucketMap = bucket.bucketMap;
//        bucketMap.putAll(bucket.bucketMap);
    }

    public BucketView createView(){
        synchronized (mapLock) {
            return new BucketView()
                    .setBucketMap(new HashMap<>(bucketMap))
                    .setIndex(index.get())
                    .setLeaderAddress(leaderAddress.get())
//                    .setLeader(isLeader.get())
                    .setVerElectId(ver.get().getElectId())
                    .setVerVotedElectId(ver.get().getVotedElectId())
                    .setVerCounter(ver.get().getCounter());
        }
    }

    public static Bucket createBucketFromView(BucketView bucketView){
        return new Bucket().replaceWithView(bucketView);
    }

    public Bucket replaceWithView(BucketView bucketView) {
        synchronized (mapLock){
            bucketMap = new ConcurrentHashMap<>(bucketView.getBucketMap());
            index.set(bucketView.getIndex());
            leaderAddress.set(bucketView.getLeaderAddress());
//            isLeader.set(bucketView.isLeader());
            ver.get()
                    .setVotedElectedId(bucketView.getVerElectId())
                    .setElectId(bucketView.getVerElectId())
                    .setCounter(bucketView.getVerCounter());
        }
        return this;
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
}
