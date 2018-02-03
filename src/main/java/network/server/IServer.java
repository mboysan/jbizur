package network.server;

public interface IServer extends Runnable{
    void init() throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
}
