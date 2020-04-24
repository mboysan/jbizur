package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.ICommand;

import java.io.Serializable;

/**
 * The generic ee.ut.jbizur.network command to _send/receive for process communication.
 */
public class NetworkCommand implements ICommand, Serializable {
    /**
     * Id to correlate with a certain context.
     */
    private Integer correlationId;
    /**
     * Id that is related to a specific context (i.e. a batch of operations).
     */
    private Integer contextId;
    /**
     * Address of the sender
     */
    private Address senderAddr;
    /**
     * Address of the receiver
     */
    private Address receiverAddr;
    /**
     * Message timestamp. Auto-generated.
     */
    private long timeStamp;
    /**
     * Any additional payload to _send.
     */
    private Serializable payload;
    /**
     * Number of times to retry sending this command in case of a failure.
     */
    private int retryCount = CoreConf.get().network.sendFailRetryCount;
    /**
     * a "member" or "client".
     */
    private String nodeType;

    private boolean isRequest = false;

    public NetworkCommand() {
        reset();
    }

    public void reset() {
        this.timeStamp = System.currentTimeMillis();
        this.retryCount = CoreConf.get().network.sendFailRetryCount;
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
     * @return gets {@link #timeStamp}
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    public Object getPayload() {
        return payload;
    }

    public NetworkCommand setPayload(Serializable payload) {
        this.payload = payload;
        return this;
    }

    public Integer getCorrelationId() {
        return correlationId;
    }

    public NetworkCommand setCorrelationId(Integer correlationId) {
        this.correlationId = correlationId;
        return this;
    }


    public Integer getContextId() {
        return contextId;
    }

    public NetworkCommand setContextId(Integer contextId) {
        this.contextId = contextId;
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

    public boolean isRequest() {
        return isRequest;
    }

    public NetworkCommand setRequest(boolean request) {
        isRequest = request;
        return this;
    }

    public NetworkCommand ofRequest(NetworkCommand requestCmd) {
        return this
                .setCorrelationId(requestCmd.getCorrelationId())
                .setContextId(requestCmd.getContextId())
                .setReceiverAddress(requestCmd.getSenderAddress());
    }

    @Override
    public String toString() {
        return "NetworkCommand{" +
                "isRequest=" + isRequest +
                ", correlationId=" + correlationId +
                ", contextId=" + contextId +
                ", senderAddr=" + senderAddr +
                ", receiverAddr=" + receiverAddr +
                ", timeStamp=" + timeStamp +
                ", payload=" + payload +
                ", retryCount=" + retryCount +
                ", nodeType='" + nodeType + '\'' +
                '}';
    }
}
