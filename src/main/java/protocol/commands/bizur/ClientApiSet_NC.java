package protocol.commands.bizur;

import protocol.commands.NetworkCommand;

public class ClientApiSet_NC extends NetworkCommand {
    private String key;
    private String val;

    public String getKey() {
        return key;
    }

    public ClientApiSet_NC setKey(String key) {
        this.key = key;
        return this;
    }

    public String getVal() {
        return val;
    }

    public ClientApiSet_NC setVal(String val) {
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
