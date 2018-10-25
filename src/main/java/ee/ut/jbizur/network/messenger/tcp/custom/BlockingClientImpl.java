package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.config.LoggerConfig;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The message sender wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingClientImpl extends AbstractClient {

    private final ExecutorService executor;
    protected static GeneralConfig.SerializationType SERIALIZATION_TYPE = GeneralConfig.getTCPSerializationType();
    /**
     * The marshaller to marshall the command to send.
     */
    protected final CommandMarshaller commandMarshaller;

    public BlockingClientImpl(Role roleInstance) {
        super(roleInstance);
        this.executor = Executors.newCachedThreadPool();
        this.commandMarshaller = new CommandMarshaller();
    }

    /**
     * {@inheritDoc}
     * Initializes the message sender. It then creates the appropriate handler to send the message.
     */
    @Override
    public void send(NetworkCommand message) {
        Runnable sender = createSender(message);
        if(message instanceof SignalEnd_NC){
            sender.run();
            executor.shutdown();
            if(executor.isShutdown()){
                if (LoggerConfig.isDebugEnabled()) {
                    Logger.debug("Client executor shutdown: "+ executor);
                }
            }
        } else {
            executor.execute(sender);
        }
    }

    private Runnable createSender(final NetworkCommand message) {
        return () -> {
            Socket socket = null;
            OutputStream out = null;
            try {
                TCPAddress receiverAddress = (TCPAddress) message.getReceiverAddress();
                socket = new Socket(receiverAddress.getIp(), receiverAddress.getPortNumber());
                out = createOutputStreamAndSend(message, socket);
            } catch (IOException e) {
                Logger.error("Send err, msg: " + message + ", " + e, e);
                roleInstance.handleInternalCommand(new SendFail_IC(message));
            } finally {
                if(out != null){
                    try {
                        out.close();
                    } catch (IOException e) {
                        Logger.error("dOut close err, msg: " + message + ", " + e, e);
                    }
                }
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Logger.error("Socket close err, msg: " + message + ", " + e, e);
                    }
                }
            }
        };
    }

    protected OutputStream createOutputStreamAndSend(NetworkCommand message, Socket socket) throws IOException {
        OutputStream out;
        switch (SERIALIZATION_TYPE) {
            case OBJECT:
                out = sendAsObject(message, socket);
                break;
            case BYTE:
                out = sendAsBytes(message, socket);
                break;
            case JSON:
                out = sendAsJSONString(message, socket);
                break;
            case STRING:
                out = sendAsString(message, socket);
                break;
            default:
                throw new UnsupportedOperationException("serialization type not supported");
        }
        out.flush();
        return out;
    }

    protected OutputStream sendAsObject(NetworkCommand message, Socket socket) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(message);
        return out;
    }

    protected OutputStream sendAsBytes(NetworkCommand message, Socket socket) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        byte[] bytesToSend = commandMarshaller.marshall(message, byte[].class);
        out.writeInt(bytesToSend.length); // write length of the message
        out.write(bytesToSend);    // write the message
        return out;
    }

    protected OutputStream sendAsJSONString(NetworkCommand message, Socket socket) throws IOException {
        throw new UnsupportedEncodingException("send as json string not supported!");
    }

    protected OutputStream sendAsString(NetworkCommand message, Socket socket) throws IOException {
        throw new UnsupportedEncodingException("send as string not supported!");
    }
}
