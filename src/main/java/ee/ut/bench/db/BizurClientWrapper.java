package ee.ut.bench.db;

import ee.ut.bench.config.Config;
import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurClient;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class BizurClientWrapper extends AbstractDBClientWrapper {

    private BizurClient client;

    @Override
    public void init() throws InterruptedException, IOException {
        BizurBuilder builder = BizurBuilder.builder();
        switch (Config.getPropsLocation()) {
            case RESOURCES:
                builder.loadPropertiesFrom(Config.class, Config.getPropsLocation().path);
                break;
            default:
                builder.loadPropertiesFrom(new File(Config.getPropsLocation().path));
        }
        client = builder.buildClient();
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
