package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

public class MulticasterMock extends Multicaster {
    public MulticasterMock(MulticastAddress multicastAddress, Role roleInstance) {
        super(multicastAddress, roleInstance);
    }

    @Override
    public void startRecv() {

    }

    @Override
    public void multicast(NetworkCommand messageToSend) {

    }

    @Override
    public void shutdown() {

    }
}
