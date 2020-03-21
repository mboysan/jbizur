package ee.ut.jbizur.network.io.vm;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.io.AbstractClient;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.SignalEnd_NC;

public class InVMClient extends AbstractClient {

    public InVMClient(String name, Address destAddress) {
        super(name, destAddress);
    }

    @Override
    protected void connect() {
    }

    @Override
    protected boolean isConnected() {
        return true;
    }

    @Override
    protected boolean isValid() {
        return true;
    }

    @Override
    public void send(NetworkCommand command) {
        if (command instanceof SignalEnd_NC) {
            InVMServer.receive(command);
        } else {
            InVMServer.receiveAsync(command);
//            InVMServer.receive(command);
        }
    }
}