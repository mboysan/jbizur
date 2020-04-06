package ee.ut.jbizur.network.io;

import ee.ut.jbizur.common.ObjectPool;
import ee.ut.jbizur.common.config.Conf;
import ee.ut.jbizur.common.protocol.address.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientPool extends ObjectPool<AbstractClient> {

    private static final Logger logger = LoggerFactory.getLogger(ClientPool.class);

    private final AtomicInteger count = new AtomicInteger(0);

    private final Address destinationAddress;

    ClientPool(Address destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    @Override
    protected AbstractClient create() {
        AbstractClient client = createClient();
        try {
            client.connect();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        return client;
    }

    private AbstractClient createClient() {
        try {
            return AbstractClient.create(
                    (Class<? extends AbstractClient>) Class.forName(Conf.get().network.client),
                    "client" + count.incrementAndGet(),
                    destinationAddress
            );
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new IllegalArgumentException("client could not be created", e);
        }
    }

    @Override
    public boolean validate(AbstractClient client) {
        return client.isValid() && client.isConnected();
    }

    @Override
    public void expire(AbstractClient client) {
        try {
            client.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
