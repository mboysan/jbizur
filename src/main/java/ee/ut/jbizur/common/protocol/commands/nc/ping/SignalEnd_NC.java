package ee.ut.jbizur.common.protocol.commands.nc.ping;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;

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
