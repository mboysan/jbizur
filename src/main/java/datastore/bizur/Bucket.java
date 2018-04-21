package datastore.bizur;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Bucket {

    private Map<String,String> bucketMap;
    private AtomicReference<Ver> ver = new AtomicReference<>(null);
    private AtomicInteger index = new AtomicInteger(0);

    public Map<String, String> getBucketMap() {
        return bucketMap;
    }

    public Bucket setBucketMap(Map<String, String> bucketMap) {
        this.bucketMap = bucketMap;
        return this;
    }

    public Ver getVer() {
        return ver.get();
    }

    public Bucket setVer(Ver ver) {
        this.ver.set(ver);
        return this;
    }

    public int getIndex() {
        return index.get();
    }

    public Bucket setIndex(int index) {
        this.index.set(index);
        return this;
    }

    public void replaceWith(Bucket bucket){
        bucket.getBucketMap().forEach((s, s2) -> bucketMap.putIfAbsent(s,s2));
        ver.set(bucket.getVer());
        index.set(bucket.getIndex());
    }
}
