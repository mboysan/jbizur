package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.common.CountdownConsumer;
import ee.ut.jbizur.common.CountdownPredicate;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CallbackListener extends CountdownConsumer<NetworkCommand> implements Predicate<NetworkCommand> {

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
        } catch (Exception ignore){}
        return true;
    }
}
