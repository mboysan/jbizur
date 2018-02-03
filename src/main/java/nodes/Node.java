package nodes;

import network.client.ClientConfig;

public class Node {

    private final NodeConfig nodeConfig;

    public Node(NodeConfig nodeConfig) {
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

    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }

    public void sendCommandToAll(Object command){

    }
}
