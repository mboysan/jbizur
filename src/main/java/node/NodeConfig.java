package node;

import network.client.ClientConfig;
import network.server.ServerConfig;

public class NodeConfig {

    private final ServerConfig serverConfig;
    private final ClientConfig[] clientConfigs;

    public NodeConfig(ServerConfig serverConfig, ClientConfig... clientConfigs) {
        this.serverConfig = serverConfig;
        this.clientConfigs = clientConfigs;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public ClientConfig[] getClientConfigs() {
        return clientConfigs;
    }
}
