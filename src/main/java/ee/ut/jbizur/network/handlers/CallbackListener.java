package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public class CallbackListener extends AbstractSyncedListener {

    private static final Logger logger = LoggerFactory.getLogger(CallbackListener.class);

    private final Consumer<NetworkCommand> cmdConsumer;

    public CallbackListener(Consumer<NetworkCommand> cmdConsumer) {
        this(0, cmdConsumer);
    }

    public CallbackListener(long timeoutMillis, Consumer<NetworkCommand> cmdConsumer) {
        super(1, timeoutMillis);
        Objects.requireNonNull(cmdConsumer);
        this.cmdConsumer = cmdConsumer;
    }

    @Override
    public boolean test(NetworkCommand command) {
        try {
            cmdConsumer.accept(command);
            countdown();
        } catch (Exception e){
            logger.warn(e.getMessage(), e);
        }
        return true;
    }
}
