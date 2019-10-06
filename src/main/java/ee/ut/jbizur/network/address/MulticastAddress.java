package ee.ut.jbizur.network.address;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Address used for node discovery.
 */
public class MulticastAddress {
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
}
