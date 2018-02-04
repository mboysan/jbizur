package protocol.commands;

import java.util.Arrays;

public class BaseCommand {
    private String senderId;
    private String[] idsToSend;
    private String receivedBy;
    private String payload;

    public String getReceivedBy() {
        return receivedBy;
    }

    public BaseCommand setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
        return this;
    }

    public String[] getIdsToSend() {
        return idsToSend;
    }

    public BaseCommand setIdsToSend(String[] idsToSend) {
        this.idsToSend = idsToSend;
        return this;
    }

    public String getSenderId() {
        return senderId;
    }

    public BaseCommand setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getPayload() {
        return payload;
    }

    public BaseCommand setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public String toString() {
        return "BaseCommand{" +
                "senderId='" + senderId + '\'' +
                ", idsToSend=" + Arrays.toString(idsToSend) +
                ", receivedBy='" + receivedBy + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
