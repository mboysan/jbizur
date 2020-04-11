package ee.ut.jbizur.protocol.commands.net;

/**
 * Signals the processes to stop waiting for further requests.
 */
public class SignalEnd_NC extends NetworkCommand{

    {
        setRequest(true);
        setCorrelationId(0);
    }

    @Override
    public String toString() {
        return "SignalEnd_NC{} " + super.toString();
    }
}
