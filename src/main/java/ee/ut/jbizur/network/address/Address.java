package ee.ut.jbizur.network.address;

import java.io.Serializable;

/**
 * Defines an abstract address for the inter-process communications
 * @see TCPAddress
 * @see MPIAddress
 */
public abstract class Address implements Serializable {

    public Address() {
    }

    public abstract String resolveAddressId();

    @Override
    public String toString() {
        return "Address{}";
    }
}
