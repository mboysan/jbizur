package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BaseListener implements Predicate<NetworkCommand> {

    private final Consumer<NetworkCommand> commandConsumer;

    public BaseListener(Consumer<NetworkCommand> commandConsumer) {
        Objects.requireNonNull(commandConsumer);
        this.commandConsumer = commandConsumer;
    }

    @Override
    public boolean test(NetworkCommand command) {
        commandConsumer.accept(command);
        return false;
    }
}
