package protocol.commands;

import network.address.Address;
import network.address.MPIAddress;
import network.address.TCPAddress;

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
     * receiver address in TCP address format
     */
    private TCPAddress receiverAddrTCP;
    /**
     * sender address in TCP address format
     */
    private TCPAddress senderAddrTCP;
    /**
     * receiver address in MPI address format
     */
    private MPIAddress receiverAddrMPI;
    /**
     * sender address in TCP address format
     */
    private MPIAddress senderAddrMPI;
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
    private String payload;

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
     * Based on the type of the <tt>senderAddress</tt> paramter, sets either the {@link #senderAddrMPI}
     * or {@link #senderAddrTCP}.
     * @param senderAddress the abstract address of the sender.
     * @return this
     */
    public NetworkCommand setSenderAddress(Address senderAddress) {
        if(senderAddress instanceof TCPAddress){
            setSenderAddrTCP((TCPAddress) senderAddress);
        } else if(senderAddress instanceof MPIAddress){
            setSenderAddrMPI((MPIAddress) senderAddress);
        }
        return this;
    }

    /**
     * Based on the type of the <tt>receiverAddress</tt> paramter, sets either the {@link #receiverAddrMPI}
     * or {@link #receiverAddrTCP}.
     * @param receiverAddress the abstract address of the receiver.
     * @return this
     */
    public NetworkCommand setReceiverAddress(Address receiverAddress) {
        if(receiverAddress instanceof TCPAddress){
            setReceiverAddrTCP((TCPAddress) receiverAddress);
        } else if(receiverAddress instanceof MPIAddress){
            setReceiverAddrMPI((MPIAddress) receiverAddress);
        }
        return this;
    }

    /**
     * Resolves the sender's address.
     * @return if sender's address is in TCP format returns {@link #getSenderAddrTCP()}, {@link #getReceiverAddrMPI()}
     *         otherwise.
     */
    public Address resolveSenderAddress() {
        if(getSenderAddrTCP() != null){
            return getSenderAddrTCP();
        } else if(getSenderAddrMPI() != null){
            return getSenderAddrMPI();
        }
        return null;
    }

    /**
     * Resolves the receiver's address.
     * @return if receiver's address is in TCP format returns {@link #getReceiverAddrTCP()} ()},
     *         {@link #getReceiverAddrMPI()} otherwise.
     */
    public Address resolveReceiverAddress() {
        if(getReceiverAddrTCP() != null){
            return getReceiverAddrTCP();
        } else if(getReceiverAddrMPI() != null){
            return getReceiverAddrMPI();
        }
        return null;
    }

    /**
     * @return gets {@link #receiverAddrTCP}
     */
    public TCPAddress getReceiverAddrTCP() {
        return receiverAddrTCP;
    }

    /**
     * @param receiverAddrTCP sets {@link #receiverAddrTCP}
     * @return this
     */
    public NetworkCommand setReceiverAddrTCP(TCPAddress receiverAddrTCP) {
        this.receiverAddrTCP = receiverAddrTCP;
        return this;
    }

    /**
     * @return gets {@link #senderAddrTCP}
     */
    public TCPAddress getSenderAddrTCP() {
        return senderAddrTCP;
    }

    /**
     * @param senderAddrTCP sets {@link #senderAddrTCP}
     * @return this
     */
    public NetworkCommand setSenderAddrTCP(TCPAddress senderAddrTCP) {
        this.senderAddrTCP = senderAddrTCP;
        return this;
    }

    /**
     * @return gets {@link #receiverAddrMPI}
     */
    public MPIAddress getReceiverAddrMPI() {
        return receiverAddrMPI;
    }

    /**
     * @param receiverAddrMPI sets {@link #receiverAddrMPI}
     * @return this
     */
    public NetworkCommand setReceiverAddrMPI(MPIAddress receiverAddrMPI) {
        this.receiverAddrMPI = receiverAddrMPI;
        return this;
    }

    /**
     * @return gets {@link #senderAddrMPI}
     */
    public MPIAddress getSenderAddrMPI() {
        return senderAddrMPI;
    }

    /**
     * @param senderAddrMPI sets {@link #senderAddrMPI}
     * @return this
     */
    public NetworkCommand setSenderAddrMPI(MPIAddress senderAddrMPI) {
        this.senderAddrMPI = senderAddrMPI;
        return this;
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
                "receiverAddrTCP=" + receiverAddrTCP +
                ", senderAddrTCP=" + senderAddrTCP +
                ", receiverAddrMPI=" + receiverAddrMPI +
                ", senderAddrMPI=" + senderAddrMPI +
                ", tag=" + tag +
                ", payload=" + payload +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
