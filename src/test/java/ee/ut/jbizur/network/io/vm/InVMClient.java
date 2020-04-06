package ee.ut.jbizur.network.io.vm;

import ee.ut.jbizur.common.protocol.address.Address;
import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.common.protocol.commands.nc.ping.SignalEnd_NC;
import ee.ut.jbizur.network.io.AbstractClient;
import ee.ut.jbizur.role.DeadNodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class InVMClient extends AbstractClient {

    private static Logger logger = LoggerFactory.getLogger(InVMClient.class);

    public InVMClient(String name, Address destAddress) {
        super(name, destAddress);
    }

    @Override
    protected void connect() {
    }

    @Override
    protected boolean isConnected() {
        return true;
    }

    @Override
    protected boolean isValid() {
        return true;
    }

    @Override
    protected void send0(NetworkCommand command) throws IOException {
        validateSend(command);
        if (DeadNodeManager.isDead(command.getReceiverAddress())) {
            // receiving node is dead. throw exception
            throw new IOException(command.toString());
        }
        if (command instanceof SignalEnd_NC) {
            InVMServer.receive(command);
        } else {
//            InVMServer.receiveAsync(command);
            InVMServer.receive(command);
        }
    }

    private static void validateSend(NetworkCommand cmd) {
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
        return "InVMClient{} " + super.toString();
    }
}