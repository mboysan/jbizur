package protocol.commands.bizur;

import protocol.commands.NetworkCommand;

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
