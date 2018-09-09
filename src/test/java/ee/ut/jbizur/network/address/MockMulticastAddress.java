package ee.ut.jbizur.network.address;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MockMulticastAddress extends MulticastAddress {
    public MockMulticastAddress(String multicastGroup, int multicastPort) throws UnknownHostException {
        super(multicastGroup, multicastPort);
    }

    public MockMulticastAddress(InetAddress multicastGroupAddr, int multicastPort) {
        super(multicastGroupAddr, multicastPort);
    }
}
