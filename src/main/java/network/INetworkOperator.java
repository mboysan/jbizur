package network;

import network.communication.IMessageHandler;

public interface INetworkOperator extends Runnable{
    IMessageHandler getMessageHandler();
    void init() throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
}