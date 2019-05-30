package ee.ut.jbizur.network.handlers;

import java.util.function.BooleanSupplier;

public class CallbackState implements IMsgState {
    private final BooleanSupplier awaitResponses;
    CallbackState(BooleanSupplier awaitResponses) {
        this.awaitResponses = awaitResponses;
    }

    public boolean awaitResponses() {
        return awaitResponses.getAsBoolean();
    }
}
