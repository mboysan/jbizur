package ee.ut.jbizur.bizur;

import java.util.Map;

public class BucketView {

    private Map<String, String> bucketMap;
    private int index;

    private int verElectId;
    private int verCounter;

    public Map<String, String> getBucketMap() {
        return bucketMap;
    }

    public BucketView setBucketMap(Map<String, String> bucketMap) {
        this.bucketMap = bucketMap;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public BucketView setIndex(int index) {
        this.index = index;
        return this;
    }

    public int getVerElectId() {
        return verElectId;
    }

    public BucketView setVerElectId(int verElectId) {
        this.verElectId = verElectId;
        return this;
    }

    public int getVerCounter() {
        return verCounter;
    }

    public BucketView setVerCounter(int verCounter) {
        this.verCounter = verCounter;
        return this;
    }

    @Override
    public String toString() {
        return "BucketView{" +
                "bucketMap=" + bucketMap +
                ", index=" + index +
                ", verElectId=" + verElectId +
                ", verCounter=" + verCounter +
                '}';
    }
}
