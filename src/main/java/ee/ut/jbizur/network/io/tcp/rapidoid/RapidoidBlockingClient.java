package ee.ut.jbizur.network.io.tcp.rapidoid;

import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.network.io.tcp.custom.BlockingClientImpl;
import ee.ut.jbizur.network.io.tcp.custom.SendSocket;

import java.io.IOException;
import java.net.Socket;

public class RapidoidBlockingClient extends BlockingClientImpl {

    public RapidoidBlockingClient(NetworkManager networkManager) {
        super(networkManager);
    }

    @Override
    protected SendSocket createSenderSocket(TCPAddress tcpAddress) throws IOException {
        return new RapidoidSendSocket(
                new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()),
                keepAlive
        );
    }
}
