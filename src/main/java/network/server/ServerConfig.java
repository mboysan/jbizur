package network.server;

import network.server.netty.NettyServer;

public class ServerConfig {

    private final boolean ssl;
    private final String address;
    private final int port;

    private final IServer server;

    public ServerConfig(int port) throws Exception {
        this(null, port);
    }

    public ServerConfig(String address, int port) throws Exception {
        this(false, address, port);
    }

    public ServerConfig(boolean ssl, String address, int port) throws Exception {
        this.ssl = ssl;
        this.address = address;
        this.port = port;

        this.server = new NettyServer(ssl, address, port);
    }

    public IServer getServer() {
        return server;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
