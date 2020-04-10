package ee.ut.jbizur.protocol.commands.net;

import ee.ut.jbizur.protocol.commands.net.NetworkCommand;

public class ClientRequest_NC extends NetworkCommand {

    {setRequest(true);}

    @Override
    public String toString() {
        return "ClientRequest_NC{} " + super.toString();
    }
}
