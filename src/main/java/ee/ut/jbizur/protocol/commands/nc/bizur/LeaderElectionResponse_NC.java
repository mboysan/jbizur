package ee.ut.jbizur.protocol.commands.nc.bizur;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

public class LeaderElectionResponse_NC extends NetworkCommand {
    private int bucketIndex;
    private boolean isSuccess;

    public int getBucketIndex() {
        return bucketIndex;
    }

    public LeaderElectionResponse_NC setBucketIndex(int bucketIndex) {
        this.bucketIndex = bucketIndex;
        return this;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public LeaderElectionResponse_NC setSuccess(boolean success) {
        isSuccess = success;
        return this;
    }

    @Override
    public String toString() {
        return "LeaderElectionResponse_NC{" +
                "bucketIndex=" + bucketIndex +
                ", isSuccess=" + isSuccess +
                "} " + super.toString();
    }
}
