package protocol.commands;

import java.util.Arrays;

public class NetworkCommand {
    private String senderId;
    private String[] idsToSend;
    private String receivedBy;
    private String payload;

    public NetworkCommand(NetworkCommand networkCommand) {
        this.senderId = networkCommand.getSenderId();
        this.idsToSend = networkCommand.getIdsToSend();
        this.receivedBy = networkCommand.getReceivedBy();
        this.payload = networkCommand.getPayload();
    }

    public NetworkCommand() {
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public NetworkCommand setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
        return this;
    }

    public String[] getIdsToSend() {
        return idsToSend;
    }

    public NetworkCommand setIdsToSend(String... idsToSend) {
        this.idsToSend = idsToSend;
        return this;
    }

    public String getSenderId() {
        return senderId;
    }

    public NetworkCommand setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getPayload() {
        return payload;
    }

    public NetworkCommand setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public String toString() {
        return "NetworkCommand{" +
                "senderId='" + senderId + '\'' +
                ", idsToSend=" + Arrays.toString(idsToSend) +
                ", receivedBy='" + receivedBy + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
