package ee.ut.jbizur.network.io.tcp.custom;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class RecvSocket extends SocketWrapper {

    public RecvSocket(Socket acceptedSocket, boolean keepAlive) throws IOException {
        super(acceptedSocket, keepAlive);
    }

    public synchronized NetworkCommand recv() throws IOException, ClassNotFoundException {
        if (inputStream instanceof ObjectInputStream) {
            return recvAsObject((ObjectInputStream) inputStream);
        } else if (inputStream instanceof DataInputStream) {
            return recvAsBytes((DataInputStream) inputStream);
        } else {
            throw new UnsupportedOperationException("input stream not recognized: " + inputStream);
        }
    }
}
