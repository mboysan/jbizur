package datastore.bizur;

import java.util.Map;

public class Bucket {

    private Map<String,String> bucketMap;
    private Ver ver;
    private int index;

    public Map<String, String> getBucketMap() {
        return bucketMap;
    }

    public Bucket setBucketMap(Map<String, String> bucketMap) {
        this.bucketMap = bucketMap;
        return this;
    }

    public Ver getVer() {
        return ver;
    }

    public Bucket setVer(Ver ver) {
        this.ver = ver;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public Bucket setIndex(int index) {
        this.index = index;
        return this;
    }
}
