package ee.ut.jbizur.protocol.commands.net;

import java.io.Serializable;

public class ApiSet_NC extends MapRequest_NC {
    {setRequest(true);}

    private Serializable key;
    private Serializable val;

    public Serializable getKey() {
        return key;
    }

    public ApiSet_NC setKey(Serializable key) {
        this.key = key;
        return this;
    }

    public Serializable getVal() {
        return val;
    }

    public ApiSet_NC setVal(Serializable val) {
        this.val = val;
        return this;
    }

    @Override
    public String toString() {
        return "ApiSet_NC{" +
                "key='" + key + '\'' +
                ", val='" + val + '\'' +
                "} " + super.toString();
    }
}
