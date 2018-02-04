package network;

import network.client.ClientConfig;
import network.client.IClient;
import network.client.netty.NettyClient;
import network.server.IServer;
import network.server.ServerConfig;
import network.server.netty.NettyServer;
import processor.CommandProcessor;
import protocol.commands.BaseCommand;
import protocol.commands.PingCommand;
import protocol.commands.internal.ClientConnectionDown;
import protocol.commands.internal.IInternalCommand;
import protocol.commands.internal.ServerDown;

import java.util.ArrayList;
import java.util.List;

public class NetworkManager {

    private final CommandProcessor commandProcessor;

    private final ServerConfig serverConfig;
    private final ClientConfig[] clientConfigs;

    private IServer server;
    private List<IClient> clients;

    public NetworkManager(CommandProcessor commandProcessor,
                          ServerConfig serverConfig,
                          ClientConfig... clientConfigs) throws Exception {
        this.commandProcessor = commandProcessor;
        this.serverConfig = serverConfig;
        this.clientConfigs = clientConfigs;
        this.clients = new ArrayList<>();

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

    public void manageAll(){
        manage(server);
        clients.forEach(this::manage);
    }

    private void manage(INetworkOperator operator){
        new Thread(operator).start();
    }

    public void notifyManager(IInternalCommand command, INetworkOperator operator){
        if(command instanceof ServerDown){
            try {
                server.stop();
                IServer newServer = registerServer(server.getServerConfig());
                manage(newServer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(command instanceof ClientConnectionDown){
            IClient client = (IClient) operator;
            try {
                client.stop();
                clients.remove(client);
                IClient newClient = registerClient(client.getClientConfig());
                manage(newClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendCommand(BaseCommand command){
        if(command instanceof PingCommand){
            clients.forEach(c -> c.getMessageHandler().sendCommand(command));
        }
    }

    public void receiveCommand(BaseCommand command){
        if(command instanceof PingCommand){
            command.setReceivedBy(commandProcessor.getNode().getNodeId());
            ((PingCommand) command).setSuccess(true);
            commandProcessor.processCommand(command);
        }
    }
}
