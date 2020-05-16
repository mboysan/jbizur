package ee.ut.jbizur.protocol.commands.net;

public class PleaseVote_NC extends MapRequest_NC {

    {setRequest(true);}

    private int bucketIndex;
    private int electId;

    public int getElectId() {
        return electId;
    }

    public PleaseVote_NC setElectId(int electId) {
        this.electId = electId;
        return this;
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    public PleaseVote_NC setBucketIndex(int bucketIndex) {
        this.bucketIndex = bucketIndex;
        return this;
    }

    @Override
    public String toString() {
        return "PleaseVote_NC{" +
                "bucketIndex=" + bucketIndex +
                ", electId=" + electId +
                "} " + super.toString();
    }
}
