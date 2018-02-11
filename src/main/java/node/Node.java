package node;

import processor.CommandProcessor;
import protocol.commands.GetNodeIdRequest;
import protocol.commands.NetworkCommand;
import protocol.commands.Ping;

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

    public void requestNodeIds(){
        NetworkCommand getNodeIdsRequest = new GetNodeIdRequest().setSenderId(nodeId);
        commandProcessor.processSend(getNodeIdsRequest);
    }

    public void sendPingToAll(){
        NetworkCommand pingCommand = new Ping()
                .setSenderId(nodeId)
                .setPayload("ping...");
        commandProcessor.processSend(pingCommand);
    }
}
