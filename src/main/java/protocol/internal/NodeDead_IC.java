package protocol.internal;

import network.address.Address;

public class NodeDead_IC extends InternalCommand {
    private final Address nodeAddress;

    public NodeDead_IC(Address nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public Address getNodeAddress() {
        return nodeAddress;
    }
}
