package ee.ut.jbizur.protocol.commands.ic;

import ee.ut.jbizur.network.address.Address;

public class AddressChange_IC extends InternalCommand {
    private final Address newAddress;
    public AddressChange_IC(Address newAddress) {
        this.newAddress = newAddress;
    }

    public Address getNewAddress() {
        return newAddress;
    }
}
