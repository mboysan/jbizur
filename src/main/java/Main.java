import network.client.ClientConfig;
import network.server.ServerConfig;
import nodes.Node;
import nodes.NodeConfig;
import protocol.commands.GenericCommand;

public class Main {

    public static void main(String[] args) throws Exception {
        Node node1 = new Node(
                new NodeConfig(
                        new ServerConfig(8001),
                        new ClientConfig("127.0.0.1", 8002),
                        new ClientConfig("127.0.0.1", 8003)
                )
        );

        Node node2 = new Node(
                new NodeConfig(
                        new ServerConfig(8002),
                        new ClientConfig("127.0.0.1", 8001),
                        new ClientConfig("127.0.0.1", 8003)
                )
        );

        Node node3 = new Node(
                new NodeConfig(
                        new ServerConfig(8003),
                        new ClientConfig("127.0.0.1", 8001),
                        new ClientConfig("127.0.0.1", 8002)
                )
        );

        //TODO: connect nodes after starting servers

        GenericCommand command = new GenericCommand().setSenderId(node1.getNodeId()).setPayload("test payload");
        node1.sendCommandToAll(command);
    }
}
