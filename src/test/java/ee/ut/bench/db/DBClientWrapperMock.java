package ee.ut.bench.db;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class DBClientWrapperMock extends AbstractDBClientWrapper {

    public AtomicInteger opCount = new AtomicInteger(0);

    public DBClientWrapperMock() {

    }

    @Override
    protected void init(String... args) throws Exception {
        reset();
    }

    @Override
    public void reset() {
        opCount.set(0);
    }

    @Override
    public <T> T set(String key, String value) {
        opCount.getAndIncrement();
        return null;
    }

    @Override
    public <T> T get(String key) {
        opCount.getAndIncrement();
        return null;
    }

    @Override
    public <T> T delete(String key) {
        opCount.getAndIncrement();
        return null;
    }

    @Override
    public Collection<String> iterateKeys() {
        opCount.getAndIncrement();
        return null;
    }
}
