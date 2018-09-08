package ee.ut.bench.db;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.BizurClient;
import ee.ut.jbizur.role.BizurNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

public class BizurClientWrapper extends AbstractDBClientWrapper {

    private BizurNode node;

    @Override
    public void init() throws InterruptedException, UnknownHostException {
        MulticastAddress multicastAddress = new MulticastAddress("all-systems.mcast.net", 9090);
        GlobalConfig.getInstance().initTCP(false, multicastAddress);

        InetAddress ip = TCPAddress.resolveIpAddress();

        node = new BizurClient(new TCPAddress(ip, 0));
    }

    @Override
    public Boolean set(String key, String value) {
        return node.set(key, value);
    }

    @Override
    public String get(String key) {
        return node.get(key);
    }

    @Override
    public Boolean delete(String key) {
        return node.delete(key);
    }

    @Override
    public Set<String> iterateKeys() {
        return node.iterateKeys();
    }

    @Override
    public void reset() {
        Set<String> keys = iterateKeys();
        for (String key : keys) {
            delete(key);
        }
    }

    @Override
    public String toString() {
        return "BizurClientWrapper";
    }
}
