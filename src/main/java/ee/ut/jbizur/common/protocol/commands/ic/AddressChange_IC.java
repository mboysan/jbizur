package ee.ut.jbizur.common.protocol.commands.ic;

import ee.ut.jbizur.common.protocol.address.Address;

public class AddressChange_IC extends InternalCommand {
    private final Address newAddress;
    public AddressChange_IC(Address newAddress) {
        this.newAddress = newAddress;
    }

    public Address getNewAddress() {
        return newAddress;
    }
}
