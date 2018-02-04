package processor;

import network.NetworkManager;
import node.Node;
import protocol.commands.BaseCommand;
import protocol.commands.PingCommand;

public class CommandProcessor {

    private final Node node;
    private final NetworkManager networkManager;

    public CommandProcessor(Node node) throws Exception {
        this.node = node;
        this.networkManager = new NetworkManager(
                this,
                node.getNodeConfig().getServerConfig(),
                node.getNodeConfig().getClientConfigs());
        init();
    }

    private void init() {
        networkManager.manageAll();
    }

    public void processCommand(BaseCommand command){
        System.out.println("Processing command: " + command.toString());
        if(command instanceof PingCommand){
            if(!((PingCommand) command).isSuccess()){
                networkManager.sendCommand(command);
            }
        }
    }

    public Node getNode() {
        return node;
    }
}
