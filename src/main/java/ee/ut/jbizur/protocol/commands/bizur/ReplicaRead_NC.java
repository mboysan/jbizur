package ee.ut.jbizur.protocol.commands.bizur;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

public class ReplicaRead_NC extends NetworkCommand {

    private int index;
    private int electId;

    public int getIndex() {
        return index;
    }

    public ReplicaRead_NC setIndex(int index) {
        this.index = index;
        return this;
    }

    public int getElectId() {
        return electId;
    }

    public ReplicaRead_NC setElectId(int electId) {
        this.electId = electId;
        return this;
    }

    @Override
    public String toString() {
        return "ReplicaRead_NC{" +
                "index=" + index +
                ", electId=" + electId +
                "} " + super.toString();
    }
}
