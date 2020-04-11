package ee.ut.jbizur.protocol.commands.intl;

import ee.ut.jbizur.protocol.address.Address;

public class NodeDead_IC extends InternalCommand {
    private final Address nodeAddress;

    public NodeDead_IC(Address nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public Address getNodeAddress() {
        return nodeAddress;
    }
}
