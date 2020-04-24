package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.protocol.commands.net.Nack_NC;

public class NackVote_NC extends Nack_NC {

    private Integer index;

    public Integer getIndex() {
        return index;
    }

    public NackVote_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "NackVote_NC{" +
                "index=" + index +
                "} " + super.toString();
    }
}
