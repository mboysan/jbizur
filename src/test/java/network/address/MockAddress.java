package network.address;

import network.address.Address;

public class MockAddress extends Address {

    private final String address;

    public MockAddress(String address) {
        this.address = address;
    }

    @Override
    public String resolveAddressId() {
        return address;
    }

    @Override
    public boolean isSame(Address other) {
        return other.resolveAddressId().equals(address);
    }

    @Override
    public String toString() {
        return "MockAddress{" +
                "address='" + address + '\'' +
                "} " + super.toString();
    }
}
