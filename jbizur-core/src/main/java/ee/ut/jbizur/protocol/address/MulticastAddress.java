package ee.ut.jbizur.protocol.address;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Address used for node discovery.
 */
public class MulticastAddress extends Address {

    private static final String SEP = ":";

    /**
     * Multicast group address
     */
    private final InetAddress multicastGroupAddr;
    /**
     * Multicast port
     */
    private final int multicastPort;

    public MulticastAddress(String multicastGroup, int multicastPort) throws UnknownHostException {
        this(InetAddress.getByName(multicastGroup), multicastPort);
    }

    public MulticastAddress(InetAddress multicastGroupAddr, int multicastPort) {
        this.multicastGroupAddr = multicastGroupAddr;
        this.multicastPort = multicastPort;
    }

    public MulticastAddress(String address) throws UnknownHostException {
        MulticastAddress maddr = resolveMulticastAddress(address);
        this.multicastGroupAddr = maddr.getMulticastGroupAddr();
        this.multicastPort = maddr.getMulticastPort();
    }

    public InetAddress getMulticastGroupAddr() {
        return multicastGroupAddr;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public static MulticastAddress resolveMulticastAddress(String multaddrStr) throws UnknownHostException {
        String[] arr = multaddrStr.split(":");
        return new MulticastAddress(arr[0], Integer.parseInt(arr[1]));
    }

    @Override
    public String resolveAddressId() {
        return multicastGroupAddr + SEP + multicastPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MulticastAddress that = (MulticastAddress) o;
        return multicastPort == that.multicastPort &&
                Objects.equals(multicastGroupAddr, that.multicastGroupAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multicastGroupAddr, multicastPort);
    }
}
