package ee.ut.jbizur.network.io.tcp.rapidoid;

import ee.ut.jbizur.network.io.tcp.custom.SendSocket;
import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.io.*;
import java.net.Socket;

public class RapidoidSendSocket extends SendSocket {
    public RapidoidSendSocket(Socket socket, boolean keepAlive)
            throws IOException {
        super(socket, keepAlive);
        commandMarshaller.setSerializer(new ByteSerializer());
    }

    protected OutputStream resolveOutStream(OutputStream out) {
        return new DataOutputStream(out);
    }

    @Override
    protected InputStream resolveInStream(InputStream in) throws IOException {
        return new DataInputStream(in);
    }

    @Override
    protected void sendAsBytes(NetworkCommand message, DataOutputStream out) throws IOException {
        byte[] msg = commandMarshaller.marshall(message, byte[].class);
        out.writeUTF(msg.length + String.format("%n"));
        out.write(msg);
        out.flush();
    }

    @Override
    protected void sendAsObject(NetworkCommand message, ObjectOutputStream objOut) {
        throw new UnsupportedOperationException("_send as object not supported!");
    }
}
