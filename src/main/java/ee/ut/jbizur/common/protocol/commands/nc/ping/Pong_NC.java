package ee.ut.jbizur.common.protocol.commands.nc.ping;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;

/**
 * Pong message
 */
public class Pong_NC extends NetworkCommand {

    @Override
    public String toString() {
        return "Pong_NC{} " + super.toString();
    }
}
