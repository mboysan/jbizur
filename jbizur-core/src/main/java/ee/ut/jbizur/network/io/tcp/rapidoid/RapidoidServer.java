package ee.ut.jbizur.network.io.tcp.rapidoid;

import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.address.TCPAddress;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import ee.ut.jbizur.network.io.AbstractServer;
import org.rapidoid.net.Server;
import org.rapidoid.net.TCP;

public class RapidoidServer extends AbstractServer {

    private Server rapidoidServer;
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    public RapidoidServer(String name, TCPAddress serverAddress) {
        super(name, serverAddress);
        commandMarshaller.setSerializer(new ByteSerializer());
    }

    @Override
    public void start() {
        rapidoidServer = TCP.server().protocol(ctx -> {
            String line = ctx.readln().trim();
            int length = Integer.parseInt(line);
            byte[] msg = ctx.input().readNbytes(length);
            NetworkCommand command = commandMarshaller.unmarshall(msg);
            recvAsync(command);
        }).port(getServerAddress().getPortNumber()).build();
        rapidoidServer.start();
        super.start();
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            if (rapidoidServer.isActive()) {
                rapidoidServer.shutdown();
            }
        }
    }

    @Override
    public TCPAddress getServerAddress() {
        return (TCPAddress) super.getServerAddress();
    }

    @Override
    public String toString() {
        return "RapidoidServer{} " + super.toString();
    }
}
