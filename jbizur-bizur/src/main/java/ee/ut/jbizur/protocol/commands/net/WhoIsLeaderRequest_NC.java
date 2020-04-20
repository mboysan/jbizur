package ee.ut.jbizur.protocol.commands.net;

public class WhoIsLeaderRequest_NC extends NetworkCommand {
    private Integer index;

    public Integer getIndex() {
        return index;
    }

    public WhoIsLeaderRequest_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "WhoIsLeaderRequest_NC{" +
                "index=" + index +
                '}';
    }
}
