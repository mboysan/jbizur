package protocol.commands.bizur;

import datastore.bizur.BucketView;
import protocol.commands.NetworkCommand;

public class AckRead_NC extends NetworkCommand {
    private BucketView bucketView;

    public BucketView getBucketView() {
        return bucketView;
    }

    public AckRead_NC setBucketView(BucketView bucketView) {
        this.bucketView = bucketView;
        return this;
    }

    @Override
    public String toString() {
        return "AckRead_NC{" +
                "bucketView=" + bucketView +
                "} " + super.toString();
    }
}
