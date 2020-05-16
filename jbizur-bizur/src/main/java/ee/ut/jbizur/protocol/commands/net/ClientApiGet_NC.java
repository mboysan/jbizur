package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class ClientApiGet_NC extends ClientRequest_NC {
    private Serializable key;

    public Serializable getKey() {
        return key;
    }

    public ClientApiGet_NC setKey(Serializable key) {
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
