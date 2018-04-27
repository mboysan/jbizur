package protocol.commands.bizur;

import datastore.bizur.BucketView;
import protocol.commands.NetworkCommand;

public class ReplicaWrite_NC extends NetworkCommand {
    private BucketView bucketView;

    public BucketView getBucketView() {
        return bucketView;
    }

    public ReplicaWrite_NC setBucketView(BucketView bucketView) {
        this.bucketView = bucketView;
        return this;
    }

    @Override
    public String toString() {
        return "ReplicaWrite_NC{" +
                "bucketView=" + bucketView +
                "} " + super.toString();
    }
}
