package ee.ut.jbizur.network.address;

import java.io.Serializable;

/**
 * Defines an abstract address for the inter-process communications
 * @see TCPAddress
 * @see MPIAddress
 */
public abstract class Address implements Serializable, Comparable<Address> {

    public Address() {
    }

    public abstract String resolveAddressId();

    @Override
    public int compareTo(Address o) {
        return Integer.compare(hashCode(), o.hashCode());
    }

    @Override
    public String toString() {
        return "Address{}";
    }
}