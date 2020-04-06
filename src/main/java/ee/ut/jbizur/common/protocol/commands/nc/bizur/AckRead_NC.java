package ee.ut.jbizur.common.protocol.commands.nc.bizur;

import ee.ut.jbizur.common.protocol.commands.nc.common.Ack_NC;

public class AckRead_NC extends Ack_NC {
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
