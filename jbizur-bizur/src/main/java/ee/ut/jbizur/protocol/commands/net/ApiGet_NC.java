package ee.ut.jbizur.protocol.commands.net;

public class ApiGet_NC extends NetworkCommand {
    {setRequest(true);}

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
