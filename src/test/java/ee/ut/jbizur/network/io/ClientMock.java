package ee.ut.jbizur.network.io;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.io.IOException;

import static org.junit.Assert.*;

public class ClientMock extends AbstractClient {

    public ClientMock(String name, Address destAddress) {
        super(name, destAddress);
    }

    @Override
    protected void connect() throws IOException {

    }

    @Override
    protected boolean isConnected() {
        return false;
    }

    @Override
    protected boolean isValid() {
        return false;
    }

    @Override
    public void send(NetworkCommand command) throws Exception {

    }
}