package ee.ut.bench.db;

import config.UserSettings;
import ee.ut.jbizur.network.address.TCPAddress;
import io.atomix.core.Atomix;
import io.atomix.core.map.DistributedMap;
import io.atomix.utils.net.Address;
import network.ConnectionProtocol;

import java.util.Collection;

public class AtomixDBClientWrapper extends AbstractDBClientWrapper {

    private Atomix client;
    private DistributedMap<String, String> distributedMap;

    @Override
    protected void init(String... args) throws Exception {
        UserSettings settings = new UserSettings(args, ConnectionProtocol.TCP_CONNECTION);

        String ipAddr = String.format("%s:%s", TCPAddress.resolveIpAddress().getHostAddress(), "0");
        String multicastAddr = String.format("%s:%s", settings.getGroupName(), settings.getGroupId());

        client = Atomix.builder()
                .withMemberId("benchmarkclient")
                .withAddress(ipAddr)
                .withMulticastEnabled()
                .withMulticastAddress(Address.from(multicastAddr))
                .build();
        distributedMap = client.getMap("benchmarkmap");
    }

    @Override
    public void reset() {
        distributedMap.forEach((s, s2) -> {
            distributedMap.remove(s);
        });
    }

    @Override
    public <T> T set(String key, String value) {
        return (T) distributedMap.put(key, value);
    }

    @Override
    public <T> T get(String key) {
        return (T) distributedMap.get(key);
    }

    @Override
    public <T> T delete(String key) {
        return (T) distributedMap.remove(key);
    }

    @Override
    public Collection<String> iterateKeys() {
        return distributedMap.keySet();
    }
}
