package network.client;

public class ClientConfig {

    private final boolean ssl;
    private final String hostAddress;
    private final int hostPort;
    private final int messageSize;

    public ClientConfig(String hostAddress, int hostPort) throws Exception {
        this(false, hostAddress, hostPort, 256);
    }

    public ClientConfig(boolean ssl, String hostAddress, int hostPort, int messageSize) {
        this.ssl = ssl;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.messageSize = messageSize;
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
