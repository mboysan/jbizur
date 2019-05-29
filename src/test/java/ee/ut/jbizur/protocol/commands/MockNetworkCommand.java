package ee.ut.jbizur.protocol.commands;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

public class MockNetworkCommand extends NetworkCommand {
    @Override
    public String toString() {
        return "MockNetworkCommand{} " + super.toString();
    }
}
