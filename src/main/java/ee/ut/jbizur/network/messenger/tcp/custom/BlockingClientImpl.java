package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractClient;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The message sender wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingClientImpl extends AbstractClient {

    private final ExecutorService executor;
    private Map<String, SocketWrapper> socketMap;

    /**
     * The marshaller to marshall the command to _send.
     */
    protected final CommandMarshaller commandMarshaller;

    public BlockingClientImpl(Role roleInstance) {
        super(roleInstance);
        this.executor = Executors.newCachedThreadPool();
        this.commandMarshaller = new CommandMarshaller();
        if (keepAlive) {
            socketMap = new ConcurrentHashMap<>();
        }
    }

    protected GeneralConfig.SerializationType getSerializationType() {
        return GeneralConfig.getTCPSerializationType();
    }

    /**
     * {@inheritDoc}
     * Initializes the message sender. It then creates the appropriate handler to _send the message.
     */
    @Override
    public void send(NetworkCommand message) {
        Runnable sender = createSender(message);
        if(message instanceof SignalEnd_NC){
            sender.run();
            shutdown();
            Logger.info("Client executor shutdown [" + executor.isShutdown() + "], info=[" + executor + "]");
        } else {
            executor.execute(sender);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        executor.shutdown();
        disconnectAll();
    }

    @Override
    protected <T> T connect(Address address) throws IOException {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address must be a TCP address");
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        SocketWrapper socketWrapper;
        if (keepAlive) {
            socketWrapper = socketMap.get(tcpAddress.toString());
            if (socketWrapper == null) {
                socketWrapper = new SocketWrapper(new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()));
                socketMap.put(tcpAddress.toString(), socketWrapper);
            }
        } else {
            socketWrapper = new SocketWrapper(new Socket(tcpAddress.getIp(), tcpAddress.getPortNumber()));
        }
        socketWrapper.socket.setKeepAlive(keepAlive);
        return (T) socketWrapper;
    }

    protected void disconnect(String tcpAddressStr, SocketWrapper socketWrapper) throws IOException {
        if (socketWrapper != null) {
            OutputStream out = socketWrapper.outputStream;
            if (out != null) {
                out.close();
            }
            socketWrapper.socket.close();
            if (keepAlive) {
                socketMap.remove(tcpAddressStr);
            }
        }
    }

    protected void disconnectAll() {
        if (socketMap == null) {
            return;
        }
        socketMap.forEach((tcpAddressStr, socket) -> {
            try {
                disconnect(tcpAddressStr, socket);
            } catch (IOException e) {
                Logger.error(e);
            }
        });
        socketMap.clear();
    }

    private Runnable createSender(final NetworkCommand message) {
        return () -> {
            SocketWrapper socketWrapper = null;
            TCPAddress receiverAddress = null;
            try {
                receiverAddress = (TCPAddress) message.getReceiverAddress();
                socketWrapper = connect(receiverAddress);
                OutputStream out = socketWrapper.outputStream;
                synchronized (out) {
                    _send(message, out);
                }
            } catch (IOException e) {
                Logger.error("Send err, msg: " + message + ", " + e, e);
                roleInstance.handleInternalCommand(new SendFail_IC(message));
            } finally {
                if (!keepAlive) {
                    try {
                        disconnect(receiverAddress.toString(), socketWrapper);
                    } catch (IOException e) {
                        Logger.error(e);
                    }
                }
            }
        };
    }

    protected void _send(NetworkCommand message, OutputStream outputStream) throws IOException {
        OutputStream out;
        switch (getSerializationType()) {
            case OBJECT:
                out = sendAsObject(message, (ObjectOutputStream) outputStream);
                break;
            case BYTE:
                out = sendAsBytes(message, (DataOutputStream) outputStream);
                break;
            case JSON:
                out = sendAsJSONString(message, outputStream);
                break;
            case STRING:
                out = sendAsString(message, outputStream);
                break;
            default:
                throw new UnsupportedOperationException("serialization type not supported");
        }
        out.flush();
    }

    protected OutputStream sendAsObject(NetworkCommand message, ObjectOutputStream out) throws IOException {
        out.writeObject(message);
        return out;
    }

    protected OutputStream sendAsBytes(NetworkCommand message, DataOutputStream out) throws IOException {
        byte[] bytesToSend = commandMarshaller.marshall(message, byte[].class);
        out.writeInt(bytesToSend.length); // write length of the message
        out.write(bytesToSend);    // write the message
        return out;
    }

    protected OutputStream sendAsJSONString(NetworkCommand message, OutputStream outputStream) throws IOException {
        throw new UnsupportedEncodingException("_send as json string not supported!");
    }

    protected OutputStream sendAsString(NetworkCommand message, OutputStream outputStream) throws IOException {
        throw new UnsupportedEncodingException("_send as string not supported!");
    }

    private class SocketWrapper {
        final Socket socket;
        final OutputStream outputStream;

        public SocketWrapper(Socket socket) throws IOException {
            this.socket = socket;
            this.outputStream = resolveOutputStreamType(socket.getOutputStream());
        }

        OutputStream resolveOutputStreamType(OutputStream outputStream) throws IOException {
            switch (getSerializationType()) {
                case OBJECT:
                    return new ObjectOutputStream(outputStream);
                case BYTE:
                    return new DataOutputStream(outputStream);
                default:
                    throw new UnsupportedOperationException("serialization type not supported");
            }
        }
    }
}
