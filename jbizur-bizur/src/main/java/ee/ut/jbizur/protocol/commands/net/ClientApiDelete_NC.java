package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class ClientApiDelete_NC extends ClientRequest_NC {
    private Serializable key;

    public Serializable getKey() {
        return key;
    }

    public ClientApiDelete_NC setKey(Serializable key) {
        this.key = key;
        return this;
    }

    @Override
    public String toString() {
        return "ClientApiDelete_NC{" +
                "key='" + key + '\'' +
                "} " + super.toString();
    }
}
