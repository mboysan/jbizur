package ee.ut.jbizur.protocol.commands.nc.ping;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

/**
 * Ping message.
 */
public class Ping_NC extends NetworkCommand {

    {setRequest(true);}

    @Override
    public String toString() {
        return "Ping_NC{} " + super.toString();
    }
}
