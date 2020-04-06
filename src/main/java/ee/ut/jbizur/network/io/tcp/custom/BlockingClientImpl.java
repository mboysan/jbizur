package ee.ut.jbizur.network.io.tcp.custom;

import ee.ut.jbizur.common.protocol.address.TCPAddress;
import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.common.protocol.commands.nc.ping.SignalEnd_NC;
import ee.ut.jbizur.network.io.AbstractClient;

import java.io.IOException;
import java.net.Socket;

import static ee.ut.jbizur.common.util.LambdaUtil.runnable;

public class BlockingClientImpl extends AbstractClient {

    private SendSocket sendSocket;

    public BlockingClientImpl(String name, TCPAddress destAddr) {
        super(name, destAddr);
    }

    @Override
    protected void connect() throws IOException {
        if (isConnected()) {
            return;
        }
        this.sendSocket = createSenderSocket(getDestAddress());
    }

    protected SendSocket createSenderSocket(TCPAddress tcpAddress) throws IOException {
        return new SendSocket(new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()),true);
    }

    @Override
    protected boolean isConnected() {
        return sendSocket != null && sendSocket.isConnected();
    }

    @Override
    protected boolean isValid() {
        return sendSocket != null;
    }

    /**
     * {@inheritDoc}
     * Initializes the message sender. It then creates the appropriate handler to _send the message.
     */
    @Override
    protected void send0(NetworkCommand command) throws Exception {
        if (!getDestAddress().equals(command.getReceiverAddress())) {
            throw new IllegalArgumentException("invalid destination="
                    + command.getReceiverAddress() + ", expected=" + getDestAddress());
        }
        if (command instanceof SignalEnd_NC) {
            try {
                sendSocket.send(command);
            } finally {
                close();
            }
        } else {
            submit(runnable(() -> sendSocket.send(command)));
        }
    }

    @Override
    public TCPAddress getDestAddress() {
        return (TCPAddress) super.getDestAddress();
    }

    @Override
    public void close() {
        super.close();
        sendSocket.close();
    }

    @Override
    public String toString() {
        return "BlockingClientImpl{} " + super.toString();
    }
}
