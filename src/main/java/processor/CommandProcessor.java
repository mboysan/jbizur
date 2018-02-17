package processor;

import network.NetworkManager;
import node.Node;
import protocol.commands.NetworkCommand;
import protocol.commands.Ping;
import protocol.commands.PingDone;

public class CommandProcessor {

    private final CommandValidator validator;

    private final Node node;
    private final NetworkManager networkManager;

    public CommandProcessor(Node node) throws Exception {
        this.node = node;
        this.networkManager = new NetworkManager(
                this,
                node.getNodeConfig().getServerConfig(),
                node.getNodeConfig().getClientConfigs());

        this.validator = new CommandValidator(node.getNodeId());

        init();
    }

    private void init() {
        networkManager.manageAll();
    }

    public void processSend(NetworkCommand command) {
        System.out.println("CommandProcessor.processSend(): " + command.toString());
        networkManager.sendCommand(command);
    }

    public void processReceive(NetworkCommand command) {
        if (!getValidator().validateCommand(command)) {
            return;
        }

        System.out.println("CommandProcessor.processReceive(): " + command.toString());
        if (command instanceof Ping) {
            NetworkCommand pingDone = new PingDone(command)
                    .setSenderId(node.getNodeId())
                    .setIdsToSend(command.getSenderId())
                    .setPayload("pingdone.");
            networkManager.sendCommand(pingDone);
        }

    }

    protected CommandValidator getValidator() {
        return validator;
    }

    public Node getNode() {
        return node;
    }
}
