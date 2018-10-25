package ee.ut.jbizur.network.messenger.tcp.rapidoid;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.network.messenger.tcp.custom.BlockingClientImpl;
import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class RapidoidBlockingClient extends BlockingClientImpl {

    public RapidoidBlockingClient(Role roleInstance) {
        super(roleInstance);
        SERIALIZATION_TYPE = GeneralConfig.SerializationType.BYTE;
        commandMarshaller.setSerializer(new ByteSerializer());
    }

    protected OutputStream sendAsBytes(NetworkCommand message, Socket socket) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        byte[] msg = commandMarshaller.marshall(message, byte[].class);
        out.writeUTF(msg.length + String.format("%n"));
        out.write(msg);
        return out;
    }

    @Override
    protected OutputStream sendAsObject(NetworkCommand message, Socket socket) throws IOException {
        throw new UnsupportedOperationException("send as object not supported!");
    }

    @Override
    protected OutputStream sendAsJSONString(NetworkCommand message, Socket socket) throws IOException {
        throw new UnsupportedOperationException("send as json not supported!");
    }

    @Override
    protected OutputStream sendAsString(NetworkCommand message, Socket socket) throws IOException {
        throw new UnsupportedOperationException("send as string not supported!");
    }
}
