package protocol.commands;

public class GenericCommand {
    private String senderId;
    private String[] idsToSend;
    private String payload;

    public String[] getIdsToSend() {
        return idsToSend;
    }

    public GenericCommand setIdsToSend(String[] idsToSend) {
        this.idsToSend = idsToSend;
        return this;
    }

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
