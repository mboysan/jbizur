package ee.ut.jbizur.network.messenger;

/**
 * An interface for receiving messages from processes.
 */
public interface IMessageReceiver {
    /**
     * Starts receiving messages with the underlying communication stack.
     */
    void startRecv();

    void shutdown();
}
