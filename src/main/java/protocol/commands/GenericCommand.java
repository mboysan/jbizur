package protocol.commands;

public class GenericCommand implements ICommand{
    private String senderId;
    private String payload;

    public String getSenderId() {
        return senderId;
    }

    public GenericCommand setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getPayload() {
        return payload;
    }

    public GenericCommand setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public String toString() {
        return "GenericCommand{" +
                "senderId='" + senderId + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
