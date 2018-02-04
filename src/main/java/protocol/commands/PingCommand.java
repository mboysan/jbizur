package protocol.commands;

public class PingCommand extends BaseCommand {
    private boolean isSuccess;

    public boolean isSuccess() {
        return isSuccess;
    }

    public PingCommand setSuccess(boolean success) {
        isSuccess = success;
        return this;
    }

    @Override
    public String toString() {
        return "PingCommand{" +
                "isSuccess=" + isSuccess +
                "} " + super.toString();
    }
}
