package protocol.commands;

import network.address.Address;

import java.io.Serializable;

/**
 * The generic network command to send/receive for process communication.
 */
public class NetworkCommand implements Serializable {

    /**
     * Id of the sender process
     */
    private String senderId;
    /**
     * Address of the sender
     */
    private Address senderAddr;
    /**
     * Address of the receiver
     */
    private Address receiverAddr;
    /**
     * Message tag
     */
    private int tag = MessageTag.ANY_TAG.getTagValue();  //set default tag
    /**
     * Message timestamp. Auto-generated.
     */
    private long timeStamp = System.currentTimeMillis();
    /**
     * Any additional payload to send.
     */
    private Object payload;

    private String msgId;

    private String assocMsgId;

    private boolean isHandled = false;

    public NetworkCommand() {

    }

    /**
     * @return gets {@link #senderId}
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * @param senderId sets {@link #senderId}
     * @return this
     */
    public NetworkCommand setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    /**
     * @param senderAddress the abstract address of the sender.
     * @return this
     */
    public NetworkCommand setSenderAddress(Address senderAddress) {
        this.senderAddr = senderAddress;
        return this;
    }

    /**
     * @param receiverAddress the abstract address of the receiver.
     * @return this
     */
    public NetworkCommand setReceiverAddress(Address receiverAddress) {
        this.receiverAddr = receiverAddress;
        return this;
    }

    /**
     * @return address of the sender.
     */
    public Address getSenderAddress() {
        return senderAddr;
    }

    /**
     * @return address of the receiver.
     */
    public Address getReceiverAddress() {
        return receiverAddr;
    }

    /**
     * @return gets {@link #tag}
     */
    public int getTag() {
        return tag;
    }

    /**
     * @param tag sets {@link #tag}
     * @return this
     */
    public NetworkCommand setTag(int tag) {
        this.tag = tag;
        return this;
    }

    /**
     * @return gets {@link #timeStamp}
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    public Object getPayload() {
        return payload;
    }

    public NetworkCommand setPayload(Object payload) {
        this.payload = payload;
        return this;
    }

    public String getMsgId() {
        return msgId;
    }

    public NetworkCommand setMsgId(String msgId) {
        this.msgId = msgId;
        return this;
    }

    public String getAssocMsgId() {
        return assocMsgId;
    }

    public NetworkCommand setAssocMsgId(String assocMsgId) {
        this.assocMsgId = assocMsgId;
        return this;
    }

    public boolean isHandled() {
        return isHandled;
    }

    public NetworkCommand setHandled(boolean handled) {
        isHandled = handled;
        return this;
    }

    @Override
    public String toString() {
        return "NetworkCommand{" +
                "senderId='" + senderId + '\'' +
                ", senderAddr=" + senderAddr +
                ", receiverAddr=" + receiverAddr +
                ", tag=" + tag +
                ", timeStamp=" + timeStamp +
                ", payload='" + payload + '\'' +
                ", msgId='" + msgId + '\'' +
                ", assocMsgId='" + assocMsgId + '\'' +
                ", isHandled=" + isHandled +
                '}';
    }
}
