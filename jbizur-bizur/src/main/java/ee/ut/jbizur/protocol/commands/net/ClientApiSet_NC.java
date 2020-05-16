package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class ClientApiSet_NC extends ClientRequest_NC {
    private Serializable key;
    private Serializable val;

    public Serializable getKey() {
        return key;
    }

    public ClientApiSet_NC setKey(Serializable key) {
        this.key = key;
        return this;
    }

    public Serializable getVal() {
        return val;
    }

    public ClientApiSet_NC setVal(Serializable val) {
        this.val = val;
        return this;
    }

    @Override
    public String toString() {
        return "ClientApiSet_NC{" +
                "key='" + key + '\'' +
                ", val='" + val + '\'' +
                "} " + super.toString();
    }
}
