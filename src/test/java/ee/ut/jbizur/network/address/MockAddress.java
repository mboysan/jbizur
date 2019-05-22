package ee.ut.jbizur.network.address;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockAddress that = (MockAddress) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return "MockAddress{" +
                "address='" + address + '\'' +
                "} " + super.toString();
    }
}
