package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.protocol.commands.net.Ack_NC;

public class AckVote_NC extends Ack_NC {

    private Integer index;

    public Integer getIndex() {
        return index;
    }

    public AckVote_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "AckVote_NC{" +
                "index=" + index +
                "} " + super.toString();
    }
}
