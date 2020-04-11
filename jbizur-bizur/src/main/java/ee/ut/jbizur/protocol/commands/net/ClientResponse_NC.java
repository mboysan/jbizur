package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.protocol.address.Address;

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
