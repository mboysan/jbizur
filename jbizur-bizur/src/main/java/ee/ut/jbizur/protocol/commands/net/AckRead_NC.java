package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class AckRead_NC extends Ack_NC {

    private Integer index;

    private BucketView<Serializable, Serializable> bucketView;

    public BucketView<Serializable, Serializable> getBucketView() {
        return bucketView;
    }

    public AckRead_NC setBucketView(BucketView<Serializable, Serializable> bucketView) {
        this.bucketView = bucketView;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public AckRead_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "AckRead_NC{" +
                "index=" + index +
                ", bucketView=" + bucketView +
                "} " + super.toString();
    }
}
