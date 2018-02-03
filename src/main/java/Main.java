import network.client.ClientConfig;
import network.server.ServerConfig;
import nodes.Node;
import nodes.NodeConfig;

public class Main {

    public static void main(String[] args) throws Exception {
        Node node1 = new Node(
                new NodeConfig(
                        new ServerConfig(8001),
                        new ClientConfig("127.0.0.1", 8002)
                )
        );

        Node node2 = new Node(
                new NodeConfig(
                        new ServerConfig(8002),
                        new ClientConfig("127.0.0.1", 8001)
                )
        );
    }
}
