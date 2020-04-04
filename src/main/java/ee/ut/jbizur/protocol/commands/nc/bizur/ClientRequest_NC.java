package ee.ut.jbizur.protocol.commands.nc.bizur;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

public class ClientRequest_NC extends NetworkCommand {

    {setRequest(true);}

    @Override
    public String toString() {
        return "ClientRequest_NC{} " + super.toString();
    }
}
