package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public abstract class AbstractServer {

    protected Role roleInstance;

    public AbstractServer(Role roleInstance) {
        this.roleInstance = roleInstance;
    }

    protected Address initAndGetAddress(Address address) {
        if (address instanceof TCPAddress) {
            TCPAddress tcpAddress = (TCPAddress) address;
            ServerSocket serverSocket = createServerSocket(tcpAddress.getPortNumber());
            tcpAddress.setIp(tcpAddress.getIp()).setPortNumber(serverSocket.getLocalPort());
            try {
                serverSocket.close();
            } catch (IOException e) {
                Logger.error(e);
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
            Logger.warn("Could not create ServerSocket on port=" + port + ", retrying with port=0... [" + e + "]");
            serverSocket = createServerSocket(0);
        }
        return serverSocket;
    }

    public abstract void startRecv(Address address);
    public abstract void shutdown();
}
