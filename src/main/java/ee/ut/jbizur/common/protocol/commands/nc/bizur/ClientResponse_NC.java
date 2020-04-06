package ee.ut.jbizur.common.protocol.commands.nc.bizur;

import ee.ut.jbizur.common.protocol.address.Address;
import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;

public class ClientResponse_NC extends NetworkCommand {
    private String request;
    private Address assumedLeaderAddress;

    public String getRequest() {
        return request;
    }

    public ClientResponse_NC setRequest(String request) {
        this.request = request;
        return this;
    }

    public Address getAssumedLeaderAddress() {
        return assumedLeaderAddress;
    }

    public ClientResponse_NC setAssumedLeaderAddress(Address assumedLeaderAddress) {
        this.assumedLeaderAddress = assumedLeaderAddress;
        return this;
    }

    @Override
    public String toString() {
        return "ClientResponse_NC{" +
                "request='" + request + '\'' +
                ", assumedLeaderAddress=" + assumedLeaderAddress +
                "} " + super.toString();
    }
}
