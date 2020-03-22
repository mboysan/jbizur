package ee.ut.jbizur.network.io.vm;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.io.AbstractServer;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InVMServer extends AbstractServer {

    private static final Map<Address, InVMServer> servers = new ConcurrentHashMap<>();

    public InVMServer(String name, Address serverAddress) {
        super(name, serverAddress);
        servers.put(serverAddress, this);
    }

    @Override
    public void close() {
        super.close();
        servers.clear();
    }

    public static void receive(NetworkCommand cmd) {
        servers.get(cmd.getReceiverAddress()).recv(cmd);
    }

    public static void receiveAsync(NetworkCommand cmd) {
        servers.get(cmd.getReceiverAddress()).recvAsync(cmd);
    }

    @Override
    public String toString() {
        return "InVMServer{} " + super.toString();
    }
}