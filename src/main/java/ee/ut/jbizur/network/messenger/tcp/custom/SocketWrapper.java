package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Stack;

public abstract class SocketWrapper {

    /**
     * The marshaller to marshall the command to _send.
     */
    protected final CommandMarshaller commandMarshaller = new CommandMarshaller();

    private final Socket socket;
    protected final OutputStream outputStream;
    protected final InputStream inputStream;

    protected final String serializationType = Conf.get().network.sendRecvAs;
    protected final boolean isBufferedIO = Conf.get().network.bufferedIO;

    protected final Stack<Closeable> closeables = new Stack<>();

    public SocketWrapper(Socket socket, boolean keepAlive) throws IOException {
        this.socket = socket;
        this.socket.setKeepAlive(keepAlive);
        closeables.push(socket);

        OutputStream sockOut = socket.getOutputStream();
        closeables.push(sockOut);
        if (isBufferedIO) {
            BufferedOutputStream bfo = new BufferedOutputStream(sockOut);
            closeables.push(bfo);
            sockOut = bfo;
        }
        this.outputStream = resolveOutStream(sockOut);
        closeables.push(outputStream);

        InputStream sockIn = socket.getInputStream();
        closeables.push(sockIn);
        if (isBufferedIO) {
            BufferedInputStream bfi = new BufferedInputStream(sockIn);
            closeables.push(bfi);
            sockIn = bfi;
        }
        this.inputStream = resolveInStream(sockIn);
        closeables.push(inputStream);
    }

    ///////////////////// SEND

    protected OutputStream resolveOutStream(OutputStream out) throws IOException {
        switch (serializationType) {
            case "OBJECT":
                return new ObjectOutputStream(out);
            case "BYTE":
                return new DataOutputStream(out);
            default:
                throw new UnsupportedOperationException("serialization type not supported");
        }
    }

    protected void sendAsObject(NetworkCommand message, ObjectOutputStream out) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    protected void sendAsBytes(NetworkCommand message, DataOutputStream out) throws IOException {
        byte[] bytesToSend = commandMarshaller.marshall(message, byte[].class);
        out.writeInt(bytesToSend.length); // write length of the message
        out.write(bytesToSend);    // write the message
        out.flush();
    }

    protected void sendAsJSONString(NetworkCommand message, OutputStream outputStream) throws IOException {
        throw new UnsupportedEncodingException("send as json string not supported!");
    }

    protected void sendAsString(NetworkCommand message, OutputStream outputStream) throws IOException {
        throw new UnsupportedEncodingException("send as string not supported!");
    }

    ///////////////////// RECV


    protected InputStream resolveInStream(InputStream in) throws IOException {
        switch (serializationType) {
            case "OBJECT":
                return new ObjectInputStream(in);
            case "BYTE":
                return new DataInputStream(in);
            default:
                throw new UnsupportedOperationException("serialization type not supported");
        }
    }

    protected NetworkCommand recvAsObject(ObjectInputStream oIn) throws IOException, ClassNotFoundException {
        return (NetworkCommand) oIn.readObject();
    }

    protected NetworkCommand recvAsBytes(DataInputStream dIn) throws IOException, ClassNotFoundException {
        int size = dIn.readInt();
        byte[] msg = new byte[size];
        final int read = dIn.read(msg);
        if (read == size) {
            return commandMarshaller.unmarshall(msg);
        } else {
            throw new IOException(String.format("Read bytes do not match the expected size:[exp=%d,act=%d]!", size, read));
        }
    }

    synchronized void close() {
        while(!closeables.isEmpty()) {
            Closeable closeable = closeables.pop();
            try {
                closeable.close();
            } catch (IOException e) {
                Logger.error(e);
            }
        }
    }

}
