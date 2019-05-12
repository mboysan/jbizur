package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class RecvSocket extends SocketWrapper {

    protected final ServerSocket serverSocket;

    public RecvSocket(Socket acceptedSocket, boolean keepAlive) throws IOException {
        this(null, acceptedSocket, keepAlive);
    }

    public RecvSocket(ServerSocket serverSocket, Socket acceptedSocket, boolean keepAlive) throws IOException {
        super(acceptedSocket, keepAlive);
        this.serverSocket = serverSocket;
        closeables.add(0, serverSocket);
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
