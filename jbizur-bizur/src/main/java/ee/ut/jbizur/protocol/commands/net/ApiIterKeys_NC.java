package ee.ut.jbizur.protocol.commands.net;

public class ApiIterKeys_NC extends MapRequest_NC {
    {setRequest(true);}

    private Integer index;

    public Integer getIndex() {
        return index;
    }

    public ApiIterKeys_NC setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "ApiIterKeys_NC{" +
                "index=" + index +
                "} " + super.toString();
    }
}
