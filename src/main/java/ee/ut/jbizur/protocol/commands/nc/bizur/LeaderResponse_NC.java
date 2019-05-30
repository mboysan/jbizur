package ee.ut.jbizur.protocol.commands.nc.bizur;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

public class LeaderResponse_NC extends NetworkCommand {
    private String request;

    public String getRequest() {
        return request;
    }

    public LeaderResponse_NC setRequest(String request) {
        this.request = request;
        return this;
    }

    @Override
    public String toString() {
        return "LeaderResponse_NC{" +
                "request='" + request + '\'' +
                "} " + super.toString();
    }
}
