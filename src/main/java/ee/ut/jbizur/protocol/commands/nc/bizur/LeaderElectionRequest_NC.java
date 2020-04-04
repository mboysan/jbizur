package ee.ut.jbizur.protocol.commands.nc.bizur;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

public class LeaderElectionRequest_NC extends NetworkCommand {

    {setRequest(true);}

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
