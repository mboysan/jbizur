package ee.ut.jbizur.datastore.bizur;

import ee.ut.jbizur.network.address.Address;

import java.io.Serializable;
import java.util.Map;

public class BucketView implements Serializable {

    private Map<String, String> bucketMap;
    private int index;

    private int verElectId;
    private int verVotedElectId;
    private int verCounter;

    private Address leaderAddress;
    private boolean isLeader;

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

    public int getVerVotedElectId() {
        return verVotedElectId;
    }

    public BucketView setVerVotedElectId(int verVotedElectId) {
        this.verVotedElectId = verVotedElectId;
        return this;
    }

    public Address getLeaderAddress() {
        return leaderAddress;
    }

    public BucketView setLeaderAddress(Address leaderAddress) {
        this.leaderAddress = leaderAddress;
        return this;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public BucketView setLeader(boolean leader) {
        isLeader = leader;
        return this;
    }

    @Override
    public String toString() {
        return "BucketView{" +
                "bucketMap=" + bucketMap +
                ", index=" + index +
                ", verElectId=" + verElectId +
                ", verVotedElectId=" + verVotedElectId +
                ", verCounter=" + verCounter +
                ", leaderAddress=" + leaderAddress +
                ", isLeader=" + isLeader +
                '}';
    }
}
