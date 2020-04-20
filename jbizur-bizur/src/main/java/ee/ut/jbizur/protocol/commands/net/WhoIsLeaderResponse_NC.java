package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.protocol.address.Address;

public class WhoIsLeaderResponse_NC extends NetworkCommand {
    private Integer index;
    private Address leaderAddress;

    public Integer getIndex() {
        return index;
    }

    public WhoIsLeaderResponse_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public Address getLeaderAddress() {
        return leaderAddress;
    }

    public WhoIsLeaderResponse_NC setLeaderAddress(Address leaderAddress) {
        this.leaderAddress = leaderAddress;
        return this;
    }


    @Override
    public String toString() {
        return "WhoIsLeaderRequest_NC{" +
                "index=" + index +
                '}';
    }
}
