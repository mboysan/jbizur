package network.client;

public class ClientConfig {

    private final boolean ssl;
    private final String hostAddress;
    private final int hostPort;

    public ClientConfig(String hostAddress, int hostPort) throws Exception {
        this(false, hostAddress, hostPort);
    }

    public ClientConfig(boolean ssl, String hostAddress, int hostPort) {
        this.ssl = ssl;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public int getHostPort() {
        return hostPort;
    }
}
