package protocol.commands;

public class PingDone extends NetworkCommand {
    public PingDone() {
    }

    public PingDone(NetworkCommand networkCommand) {
        super(networkCommand);
    }

    @Override
    public String toString() {
        return "PingDone{} " + super.toString();
    }
}
