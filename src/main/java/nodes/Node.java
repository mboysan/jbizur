package nodes;

import processor.CommandProcessor;
import protocol.commands.BaseCommand;
import protocol.commands.PingCommand;

import java.util.UUID;

public class Node {

    private final String nodeId;

    private final NodeConfig nodeConfig;

    private final CommandProcessor commandProcessor;

    public Node(NodeConfig nodeConfig) throws Exception {
        this(UUID.randomUUID().toString(), nodeConfig);
    }

    public Node(String nodeId, NodeConfig nodeConfig) throws Exception {
        this.nodeId = nodeId;
        this.nodeConfig = nodeConfig;
        this.commandProcessor = new CommandProcessor(this);
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }

    public void sendPingToAll(){
        BaseCommand pingCommand = new PingCommand()
                .setSuccess(false)
                .setSenderId(nodeId)
                .setPayload("ping...");
        commandProcessor.processCommand(pingCommand);
    }
}
