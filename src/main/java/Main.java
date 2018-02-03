import communication.server.IServer;
import communication.server.netty.NettyServer;

public class Main {

    public static void main(String[] args) throws Exception {
        IServer server = new NettyServer();
        server.start();

        System.out.printf("asdsadsadsa");
    }
}
