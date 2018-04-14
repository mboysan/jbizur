package network.address;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Address used for process discovery. Used internally by {@link config.GlobalConfig} and is not exposed to nodes.
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

    public InetAddress getMulticastGroupAddr() {
        return multicastGroupAddr;
    }

    public int getMulticastPort() {
        return multicastPort;
    }
}
