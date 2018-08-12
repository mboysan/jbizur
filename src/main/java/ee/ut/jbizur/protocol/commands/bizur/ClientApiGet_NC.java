package ee.ut.jbizur.protocol.commands.bizur;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

public class ClientApiGet_NC extends NetworkCommand {
    private String key;

    public String getKey() {
        return key;
    }

    public ClientApiGet_NC setKey(String key) {
        this.key = key;
        return this;
    }

    @Override
    public String toString() {
        return "ClientApiGet_NC{" +
                "key='" + key + '\'' +
                "} " + super.toString();
    }
}
