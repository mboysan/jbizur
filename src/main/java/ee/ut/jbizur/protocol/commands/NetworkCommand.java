package ee.ut.jbizur.protocol.commands;

import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.network.address.Address;

import java.io.Serializable;

/**
 * The generic ee.ut.jbizur.network command to send/receive for process communication.
 */
public class NetworkCommand implements Serializable {
    /**
     * Message id associated with this command.
     */
    private Integer msgId;
    private Integer contextId;
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
    private int tag;
    /**
     * Message timestamp. Auto-generated.
     */
    private long timeStamp;
    /**
     * Any additional payload to send.
     */
    private Object payload;
    /**
     * To determine if this command is handled or not.
     */
    private boolean isHandled;
    /**
     * Number of times to retry sending this command in case of a failure.
     */
    private int retryCount = BizurConfig.getSendFailRetryCount();
    /**
     * a "member" or "client".
     */
    private String nodeType;

    public NetworkCommand() {
        reset();
    }

    public void reset() {
        this.timeStamp = System.currentTimeMillis();
        this.tag = MessageTag.ANY_TAG.getTagValue();
        this.isHandled = false;
        this.retryCount = NodeConfig.getSendFailRetryCount();
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

    public Integer getMsgId() {
        return msgId;
    }

    public NetworkCommand setMsgId(Integer msgId) {
        this.msgId = msgId;
        return this;
    }

    public Integer getContextId() {
        return contextId;
    }

    public NetworkCommand setContextId(Integer contextId) {
        this.contextId = contextId;
        return this;
    }

    public boolean isHandled() {
        return isHandled;
    }

    public NetworkCommand setHandled(boolean handled) {
        isHandled = handled;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public NetworkCommand setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public String getNodeType() {
        return nodeType;
    }

    public NetworkCommand setNodeType(String nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    @Override
    public String toString() {
        return "NetworkCommand{" +
                "msgId=" + msgId +
                ", senderId='" + senderId + '\'' +
                ", senderAddr=" + senderAddr +
                ", receiverAddr=" + receiverAddr +
                ", tag=" + tag +
                ", timeStamp=" + timeStamp +
                ", payload=" + payload +
                ", isHandled=" + isHandled +
                ", retryCount=" + retryCount +
                ", nodeType='" + nodeType + '\'' +
                '}';
    }
}
