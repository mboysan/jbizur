package ee.ut.bench.db;

import ee.ut.bench.config.Config;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;

import java.util.Set;

public class BizurClientWrapper extends AbstractDBClientWrapper {

    private BizurClient client;

    @Override
    public void init() throws InterruptedException {
        client = BizurBuilder.builder()
                .loadPropertiesFrom(Config.class, "jbizur.properties")
                .buildClient();
        client.start().join();
    }

    @Override
    public Boolean set(String key, String value) {
        return client.set(key, value);
    }

    @Override
    public String get(String key) {
        return client.get(key);
    }

    @Override
    public Boolean delete(String key) {
        return client.delete(key);
    }

    @Override
    public Set<String> iterateKeys() {
        return client.iterateKeys();
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
