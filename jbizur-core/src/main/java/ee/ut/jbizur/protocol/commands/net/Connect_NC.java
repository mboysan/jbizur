package ee.ut.jbizur.protocol.commands.net;

/**
 * Connection request
 */
public class Connect_NC extends NetworkCommand {

    {
        setRequest(true);
        setCorrelationId(0);
    }

    @Override
    public String toString() {
        return "Connect_NC{} " + super.toString();
    }
}
