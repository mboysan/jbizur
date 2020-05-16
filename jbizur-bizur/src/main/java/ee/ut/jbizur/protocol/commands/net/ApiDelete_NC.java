package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class ApiDelete_NC extends MapRequest_NC {
    {setRequest(true);}

    private Serializable key;

    public Serializable getKey() {
        return key;
    }

    public ApiDelete_NC setKey(Serializable key) {
        this.key = key;
        return this;
    }

    @Override
    public String toString() {
        return "ApiDelete_NC{" +
                "key='" + key + '\'' +
                "} " + super.toString();
    }
}
