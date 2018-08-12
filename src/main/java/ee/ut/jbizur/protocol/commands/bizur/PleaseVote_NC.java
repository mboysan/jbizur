package ee.ut.jbizur.protocol.commands.bizur;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

public class PleaseVote_NC extends NetworkCommand {
    private int electId;

    public int getElectId() {
        return electId;
    }

    public PleaseVote_NC setElectId(int electId) {
        this.electId = electId;
        return this;
    }

    @Override
    public String toString() {
        return "PleaseVote_NC{" +
                "electId=" + electId +
                "} " + super.toString();
    }
}
