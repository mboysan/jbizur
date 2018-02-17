package processor;

import protocol.commands.NetworkCommand;

import java.util.Arrays;

public class CommandValidator {

    private final String nodeId;

    public CommandValidator(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean validateCommand(NetworkCommand command){
        boolean isRelevant = true;
        if(command.getIdsToSend() != null){
            String isRelevantToMe = Arrays.stream(command.getIdsToSend())
                    .filter(s -> s.equals(nodeId))
                    .findFirst()
                    .orElse(null);
            isRelevant = isRelevantToMe != null;
        }
        return isRelevant;
    }
}
