package network.client;

import network.client.netty.NettyClient;

public class ClientConfig {

    private final boolean ssl;
    private final String hostAddress;
    private final int hostPort;
    private final int messageSize;

    private final IClient client;

    public ClientConfig(String hostAddress, int hostPort) throws Exception {
        this(false, hostAddress, hostPort, 256);
    }

    public ClientConfig(boolean ssl, String hostAddress, int hostPort, int messageSize) throws Exception {
        this.ssl = ssl;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.messageSize = messageSize;

        client = new NettyClient(ssl, hostAddress, hostPort, messageSize);
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

    public int getMessageSize() {
        return messageSize;
    }

    public IClient getClient() {
        return client;
    }
}
