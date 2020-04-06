package ee.ut.jbizur.network.io.tcp.rapidoid;

import ee.ut.jbizur.common.protocol.address.TCPAddress;
import ee.ut.jbizur.network.io.tcp.custom.BlockingClientImpl;
import ee.ut.jbizur.network.io.tcp.custom.SendSocket;

import java.io.IOException;
import java.net.Socket;

public class RapidoidBlockingClient extends BlockingClientImpl {

    public RapidoidBlockingClient(String name, TCPAddress destAddr) {
        super(name, destAddr);
    }

    @Override
    protected SendSocket createSenderSocket(TCPAddress tcpAddress) throws IOException {
        return new RapidoidSendSocket(new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()),true);
    }
}
