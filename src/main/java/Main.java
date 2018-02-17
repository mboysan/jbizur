import network.client.ClientConfig;
import network.server.ServerConfig;
import node.Node;
import node.NodeConfig;

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

        Thread.sleep(5000);

//        node1.requestNodeIds();
        node1.sendPingToAll();
    }
}
