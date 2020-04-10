package ee.ut.jbizur.protocol.commands.net;

public class ReplicaWrite_NC extends NetworkCommand {

    {setRequest(true);}

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
