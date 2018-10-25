package ee.ut.jbizur.network.messenger.tcp.rapidoid;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractServer;
import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;
import org.rapidoid.net.Server;
import org.rapidoid.net.TCP;

public class RapidoidServer extends AbstractServer {
    private Server rapidoidServer;
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    public RapidoidServer(Role roleInstance) {
        super(roleInstance);
        commandMarshaller.setSerializer(new ByteSerializer());
    }

    @Override
    public void startRecv(Address address) {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address is not a TCP address: " + address);
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        rapidoidServer = TCP.server().protocol(ctx -> {
            String line = ctx.readln().trim();
            int length = Integer.parseInt(line);
            byte[] msg = ctx.input().readNbytes(length);
            NetworkCommand command = commandMarshaller.unmarshall(msg);
            roleInstance.handleNetworkCommand(command);
        }).port(tcpAddress.getPortNumber()).build();
        rapidoidServer.start();
    }

    @Override
    public void shutdown() {
        if (rapidoidServer.isActive()) {
            rapidoidServer.shutdown();
        }
    }
}
