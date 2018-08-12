package ee.ut.jbizur.protocol.commands.bizur;

import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.protocol.commands.NetworkCommand;

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
