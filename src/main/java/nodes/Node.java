package nodes;

import network.client.ClientConfig;
import protocol.commands.GenericCommand;

import java.util.UUID;

public class Node {

    private final String nodeId;

    private final NodeConfig nodeConfig;

    public Node(NodeConfig nodeConfig) {
        this(UUID.randomUUID().toString(), nodeConfig);
    }

    public Node(String nodeId, NodeConfig nodeConfig){
        this.nodeId = nodeId;
        this.nodeConfig = nodeConfig;
        init();
    }

    private void init() {
        // start server
        new Thread(nodeConfig.getServerConfig().getServer()).start();

        // start clients
        for (ClientConfig clientConfig : nodeConfig.getClientConfigs()) {
            new Thread(clientConfig.getClient()).start();
        }
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }

    public void sendCommandToAll(GenericCommand command){
        for (ClientConfig clientConfig : nodeConfig.getClientConfigs()) {
            clientConfig.getClient().getMessageHandler().sendCommand(command);
        }
    }
}
