package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.protocol.commands.net.NetworkCommand;

public class ApiIterKeys_NC extends NetworkCommand {
    private Integer index;

    public Integer getIndex() {
        return index;
    }

    public ApiIterKeys_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "ApiIterKeys_NC{" +
                "index=" + index +
                "} " + super.toString();
    }
}
