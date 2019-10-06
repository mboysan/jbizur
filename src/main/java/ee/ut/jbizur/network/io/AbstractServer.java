package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public abstract class AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    protected volatile boolean isRunning = true;

    protected final NetworkManager networkManager;
    protected final boolean keepAlive;

    public AbstractServer(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.keepAlive = Conf.get().network.tcp.keepalive;
    }

    protected Address initAndGetAddress(Address address) {
        if (address instanceof TCPAddress) {
            TCPAddress tcpAddress = (TCPAddress) address;
            ServerSocket serverSocket = createServerSocket(tcpAddress.getPortNumber());
            tcpAddress.setIp(tcpAddress.getIp()).setPortNumber(serverSocket.getLocalPort());
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            return address;
        }
        return address; //unmodified
    }

    private ServerSocket createServerSocket(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            logger.warn("Could not create ServerSocket on port={}, retrying with port=0... [{}]", port, e.getMessage());
            serverSocket = createServerSocket(0);
        }
        return serverSocket;
    }

    public abstract void startRecv(Address address);
    public abstract void shutdown();
}
