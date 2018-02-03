package network.client;

import network.communication.IMessageHandler;

public interface IClient extends Runnable{
    IMessageHandler getMessageHandler();
    void init() throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
}