package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class ReplicaWrite_NC extends MapRequest_NC {

    {setRequest(true);}

    private BucketView<Serializable, Serializable> bucketView;

    public BucketView<Serializable, Serializable> getBucketView() {
        return bucketView;
    }

    public ReplicaWrite_NC setBucketView(BucketView<Serializable, Serializable> bucketView) {
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
