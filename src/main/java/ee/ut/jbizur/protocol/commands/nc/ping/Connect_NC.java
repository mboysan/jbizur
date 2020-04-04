package ee.ut.jbizur.protocol.commands.nc.ping;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

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
