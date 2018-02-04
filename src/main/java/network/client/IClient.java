package network.client;

import network.INetworkOperator;

public interface IClient extends INetworkOperator {
    ClientConfig getClientConfig();
}