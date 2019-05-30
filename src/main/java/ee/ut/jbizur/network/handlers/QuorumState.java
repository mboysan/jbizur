package ee.ut.jbizur.network.handlers;

import java.util.function.BooleanSupplier;

public class QuorumState implements IMsgState {
    private final BooleanSupplier majorityDecision;

    QuorumState(BooleanSupplier majorityDecision) {
        this.majorityDecision = majorityDecision;
    }

    public boolean awaitMajority() {
        return majorityDecision.getAsBoolean();
    }
}
