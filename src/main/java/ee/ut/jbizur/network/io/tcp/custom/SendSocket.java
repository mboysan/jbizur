package ee.ut.jbizur.network.io.tcp.custom;

import ee.ut.jbizur.protocol.commands.NetworkCommand;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class SendSocket extends SocketWrapper {

    public SendSocket(Socket socket, boolean keepAlive) throws IOException {
        super(socket, keepAlive);
    }

    public synchronized void send(NetworkCommand message) throws IOException {
        if (outputStream instanceof ObjectOutputStream) {
            sendAsObject(message, (ObjectOutputStream) outputStream);
        } else if (outputStream instanceof DataOutputStream) {
            sendAsBytes(message, (DataOutputStream) outputStream);
        } else {
            throw new UnsupportedEncodingException("cannot send, stream not recognized: " + outputStream);
        }
    }
}
