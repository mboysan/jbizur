package network;

import network.client.ClientConfig;
import network.client.IClient;
import network.client.netty.NettyClient;
import network.server.IServer;
import network.server.ServerConfig;
import network.server.netty.NettyServer;
import processor.CommandProcessor;
import protocol.commands.NetworkCommand;
import protocol.commands.internal.ClientConnectionDown;
import protocol.commands.internal.ClientConnectionReady;
import protocol.commands.internal.InternalCommand;
import protocol.commands.internal.ServerDown;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkManager {

    private final CommandProcessor commandProcessor;

    private final ServerConfig serverConfig;
    private final ClientConfig[] clientConfigs;

    private IServer server;
    private final List<IClient> clients = new CopyOnWriteArrayList<>();

    public NetworkManager(CommandProcessor commandProcessor,
                          ServerConfig serverConfig,
                          ClientConfig... clientConfigs) throws Exception {
        this.commandProcessor = commandProcessor;
        this.serverConfig = serverConfig;
        this.clientConfigs = clientConfigs;

        registerServer(serverConfig);
        registerClients(clientConfigs);
    }

    private IServer registerServer(ServerConfig serverConfig) throws Exception {
        server = new NettyServer(this, serverConfig);
        return server;
    }

    private List<IClient> registerClients(ClientConfig[] clientConfigs) throws Exception {
        for (ClientConfig clientConfig : clientConfigs) {
            registerClient(clientConfig);
        }
        return clients;
    }

    private IClient registerClient(ClientConfig clientConfig) throws Exception {
        IClient client = new NettyClient(this, clientConfig);
        clients.add(client);
        return client;
    }

    public void manageAll() {
        manage(server);
        clients.forEach(this::manage);
    }

    private void manage(INetworkOperator operator) {
        new Thread(operator).start();
    }

    public void notifyManager(InternalCommand command, INetworkOperator operator) {
        if (command instanceof ServerDown) {
            try {
                server.stop();
                IServer newServer = registerServer(server.getServerConfig());
                manage(newServer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (command instanceof ClientConnectionDown) {
            IClient client = (IClient) operator;
            try {
                client.stop();
                clients.remove(client);
                IClient newClient = registerClient(client.getClientConfig());
                manage(newClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (command instanceof ClientConnectionReady){
            //do something?
        }
    }

    public void sendCommand(NetworkCommand command) {
        /* Send command to all */
        for (IClient client : clients) {
            sendCommand(command, client);
        }
    }

    private void sendCommand(NetworkCommand command, IClient client) {
        if (client == null) {
            return;
        }
        client.notifyOperator(command, this);
    }

    public void receiveCommand(NetworkCommand command, INetworkOperator operator) {
        commandProcessor.processReceive(command);
    }
}
