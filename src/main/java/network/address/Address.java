package network.address;

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

    /**
     * @param other other address to check if it is the same with this one.
     * @return true if this and other matches, false otherwise.
     */
    public abstract boolean isSame(Address other);

    @Override
    public String toString() {
        return "Address{}";
    }
}
