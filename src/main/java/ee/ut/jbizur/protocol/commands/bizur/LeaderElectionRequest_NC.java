package ee.ut.jbizur.protocol.commands.bizur;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

public class LeaderElectionRequest_NC extends NetworkCommand {
    private int bucketIndex;

    public int getBucketIndex() {
        return bucketIndex;
    }
    public LeaderElectionRequest_NC setBucketIndex(int bucketIndex) {
        this.bucketIndex = bucketIndex;
        return this;
    }

    @Override
    public String toString() {
        return "LeaderElectionRequest_NC{" +
                "bucketIndex=" + bucketIndex +
                "} " + super.toString();
    }
}
