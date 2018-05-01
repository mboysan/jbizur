package protocol.commands.bizur;

import protocol.commands.NetworkCommand;

public class ApiGet_NC extends NetworkCommand {
    private String key;

    public String getKey() {
        return key;
    }

    public ApiGet_NC setKey(String key) {
        this.key = key;
        return this;
    }

    @Override
    public String toString() {
        return "ApiGet_NC{" +
                "key='" + key + '\'' +
                "} " + super.toString();
    }
}
