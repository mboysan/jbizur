package ee.ut.bench.db;

import ee.ut.bench.config.AtomixConfig;
import io.atomix.core.Atomix;
import io.atomix.core.map.AtomicMap;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.profile.Profile;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.utils.net.Address;

import java.util.Collection;

public class AtomixDBClientWrapper extends AbstractDBClientWrapper {

    private Atomix client;
    private DistributedMap<String, String> keyValueStore;

    @Override
    protected void init() {
        String id = AtomixConfig.getClientId();
        String address = AtomixConfig.compileClientTCPAddress();
        String multicastAddr = AtomixConfig.compileMulticastAddress();

        client = Atomix.builder()
                .withMemberId(id)
                .withAddress(address)
                .withMulticastEnabled()
                .withMulticastAddress(Address.from(multicastAddr))
                .withProfiles(Profile.client())
                .build();
        client.start().join();
//        keyValueStore = client.getAtomicMap("benchmarkmap");
        keyValueStore = createDistributedMap("benchmarkmap");
    }

    private AtomicMap<String, String> createAtomicMap(String name) {
        if (!client.isRunning()) {
            throw new IllegalStateException("client should be initialized first");
        }
        MultiRaftProtocol protocol = MultiRaftProtocol.builder()
                .withReadConsistency(ReadConsistency.LINEARIZABLE)
                .build();
        return client
                .<String, String>atomicMapBuilder(name)
                .withProtocol(protocol)
                .build();
    }

    private DistributedMap<String, String> createDistributedMap(String name) {
        if (!client.isRunning()) {
            throw new IllegalStateException("client should be initialized first");
        }
        return client.getMap(name);
    }

    @Override
    public void reset() {
        for (String key : keyValueStore.keySet()) {
            keyValueStore.remove(key);
        }
    }

    @Override
    public <T> T set(String key, String value) {
        return (T) keyValueStore.put(key, value);
    }

    @Override
    public <T> T get(String key) {
        return (T) keyValueStore.get(key);
    }

    @Override
    public <T> T delete(String key) {
        return (T) keyValueStore.remove(key);
    }

    @Override
    public Collection<String> iterateKeys() {
        return keyValueStore.keySet();
    }
}
