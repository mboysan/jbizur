package ee.ut.jbizur.datastore.bizur;

import org.pmw.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Bucket {

    private final BucketLock bucketLock = new BucketLock();

    private final Object mapLock = new Object();
    private Map<String,String> bucketMap = new ConcurrentHashMap<>();

    private final AtomicReference<Ver> ver = new AtomicReference<>(new Ver().setElectId(0).setCounter(0));
    private final AtomicInteger index = new AtomicInteger(0);

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
                    .setVerElectId(ver.get().getElectId())
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
            ver.get().setElectId(bucketView.getVerElectId());
            ver.get().setCounter(bucketView.getVerCounter());
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
