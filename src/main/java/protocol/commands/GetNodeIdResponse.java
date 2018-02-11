package protocol.commands;

public class GetNodeIdResponse extends NetworkCommand {
    public GetNodeIdResponse(NetworkCommand networkCommand) {
        super(networkCommand);
    }

    public GetNodeIdResponse() {
    }

    @Override
    public String toString() {
        return "GetNodeIdResponse{} " + super.toString();
    }
}
