package network.server;

public class ServerConfig {

    private final boolean ssl;
    private final String address;
    private final int port;

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
