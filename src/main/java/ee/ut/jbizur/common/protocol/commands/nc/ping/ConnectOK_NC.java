package ee.ut.jbizur.common.protocol.commands.nc.ping;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;

/**
 * Connection OK response
 */
public class ConnectOK_NC extends NetworkCommand {

    {
        setCorrelationId(0);
    }

    @Override
    public String toString() {
        return "ConnectOK_NC{} " + super.toString();
    }
}
