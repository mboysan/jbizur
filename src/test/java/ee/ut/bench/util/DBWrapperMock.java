package ee.ut.bench.util;

import java.util.Collection;

public class DBWrapperMock extends AbstractDBWrapper {

    public DBWrapperMock() {

    }

    @Override
    protected void init(String... args) throws Exception {
    }

    @Override
    public void reset() {
    }

    @Override
    public <T> T set(String key, String value) {
        return null;
    }

    @Override
    public <T> T get(String key) {
        return null;
    }

    @Override
    public <T> T delete(String key) {
        return null;
    }

    @Override
    public Collection<String> iterateKeys() {
        return null;
    }
}
