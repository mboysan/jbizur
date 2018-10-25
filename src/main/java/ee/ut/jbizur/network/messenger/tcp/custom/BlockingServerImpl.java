package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.config.LoggerConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractServer;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The message receiver wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingServerImpl extends AbstractServer {

    private volatile boolean isRunning = true;

    private final ExecutorService executor;
    private final static GeneralConfig.SerializationType SERIALIZATION_TYPE = GeneralConfig.getTCPSerializationType();

    /**
     * Command marshaller to unmarshall the received message.
     */
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    public BlockingServerImpl(Role roleInstance) {
        super(roleInstance);
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public void shutdown() {
        this.isRunning = false;
        executor.shutdown();
        if(executor.isShutdown()){
            if (LoggerConfig.isDebugEnabled()) {
                Logger.debug("Server executor shutdown: "+ executor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRecv(Address address){
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address is not a TCP address: " + address);
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        Runnable receiver = createReceiver(tcpAddress.getPortNumber());
        new Thread(receiver).start();
    }

    private Runnable createReceiver(final int port) {
        return () -> {
            ServerSocket serverSocket;
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(port);
                while (isRunning) {
                    socket = serverSocket.accept();
                    Object message = null;

                    switch (SERIALIZATION_TYPE) {
                        case OBJECT:
                            ObjectInputStream oIs = new ObjectInputStream(socket.getInputStream());
                            message = oIs.readObject();
                            break;
                        case BYTE:
                            DataInputStream dIn = new DataInputStream(socket.getInputStream());
                            int size = dIn.readInt();
                            byte[] msg = new byte[size];
                            dIn.read(msg);
                            message = commandMarshaller.unmarshall(msg);
                            break;
                    }

                    if(message != null){
                        NetworkCommand command = (NetworkCommand) message;
                        executor.execute(() -> {
                            roleInstance.handleNetworkCommand(command);
                        });
                    }
                }
                if (LoggerConfig.isDebugEnabled()) {
                    Logger.debug("receiver end");
                }
            } catch (Exception e) {
                Logger.error(e, "Recv err");
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception ex) {
                    Logger.error(ex, "Socket close err");
                }
            }
        };
    }
}