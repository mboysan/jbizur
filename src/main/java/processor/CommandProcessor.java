package processor;

import network.NetworkManager;
import node.Node;
import protocol.commands.*;

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

    public void processSend(NetworkCommand command){
        System.out.println("CommandProcessor.processSend(): " + command.toString());
        networkManager.sendCommand(command);
    }

    public void processReceive(NetworkCommand command){
        System.out.println("CommandProcessor.processReceive(): " + command.toString());
        if(command instanceof GetNodeIdRequest){
            NetworkCommand getNodeIdResponse = new GetNodeIdResponse(command)
                    .setSenderId(node.getNodeId());
            networkManager.sendCommand(getNodeIdResponse);
        } else if(command instanceof Ping){
            NetworkCommand pingDone = new PingDone(command)
                    .setSenderId(node.getNodeId())
                    .setIdsToSend(command.getSenderId())
                    .setPayload("pingdone.");
            networkManager.sendCommand(pingDone);
        }
    }

    public Node getNode() {
        return node;
    }
}
