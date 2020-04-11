package ee.ut.jbizur.network.io.vm;

import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import ee.ut.jbizur.network.io.AbstractServer;
import ee.ut.jbizur.role.DeadNodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InVMServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(InVMServer.class);

    private static final Map<Address, InVMServer> SERVERS = new ConcurrentHashMap<>();

    public InVMServer(String name, Address serverAddress) {
        super(name, serverAddress);
        SERVERS.put(serverAddress, this);
    }

    @Override
    public void close() {
        super.close();
        SERVERS.clear();
    }

    public static void receive(NetworkCommand cmd) {
        validateReceive(cmd);
/*        if (DeadNodeManager.isDead(cmd.getReceiverAddress())) {
            // receiver is dead, send Nack to sender.
            SERVERS.get(cmd.getSenderAddress()).recv(new Nack_NC().ofRequest(cmd));
        } else {
            SERVERS.get(cmd.getReceiverAddress()).recv(cmd);
        }*/
        SERVERS.get(cmd.getReceiverAddress()).recv(cmd);
    }

    public static void receiveAsync(NetworkCommand cmd) {
        validateReceive(cmd);
/*        if (DeadNodeManager.isDead(cmd.getReceiverAddress())) {
            // receiver is dead, send Nack to sender.
            SERVERS.get(cmd.getSenderAddress()).recvAsync(new Nack_NC().ofRequest(cmd));
        } else {
            SERVERS.get(cmd.getReceiverAddress()).recvAsync(cmd);
        }*/
        SERVERS.get(cmd.getReceiverAddress()).recvAsync(cmd);
    }

    private static void validateReceive(NetworkCommand cmd) {
        if (DeadNodeManager.isDead(cmd.getSenderAddress())) {
            // sender is dead.
            throw new IllegalArgumentException("node is dead, cannot send. cmd=" + cmd);
        }
        if (DeadNodeManager.isDead(cmd.getReceiverAddress())) {
            logger.warn("receiver node is dead. cmd=" + cmd);
        }
    }

    @Override
    public String toString() {
        return "InVMServer{} " + super.toString();
    }
}