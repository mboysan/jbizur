package ee.ut.jbizur.protocol.commands.nc.bizur;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

public class ApiSet_NC extends NetworkCommand {
    private String key;
    private String val;

    public String getKey() {
        return key;
    }

    public ApiSet_NC setKey(String key) {
        this.key = key;
        return this;
    }

    public String getVal() {
        return val;
    }

    public ApiSet_NC setVal(String val) {
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
