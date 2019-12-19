package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class Listeners {

    private final Map<Integer, Predicate<NetworkCommand>> listeners = new ConcurrentHashMap<>();

    public Listeners() {}

    public void add(int id, Predicate<NetworkCommand> listener) {
        listeners.put(id, listener);
    }

    public void handle(NetworkCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Integer correlationId = command.getCorrelationId();
        Predicate<NetworkCommand> listener;
        if (correlationId == null || correlationId <= 0) {
            listener = listeners.get(0);
        } else {
            listener = listeners.get(correlationId);
        }
        if (listener != null) {
            if (listener.test(command)) {
                listeners.remove(correlationId);
            }
        }
    }

    Map<Integer, Predicate<NetworkCommand>> getListeners() {
        return Collections.unmodifiableMap(listeners);
    }
}
