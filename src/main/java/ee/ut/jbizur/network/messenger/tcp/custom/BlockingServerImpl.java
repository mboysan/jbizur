package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractServer;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The message receiver wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingServerImpl extends AbstractServer {

    private Thread serverThread;
    private final ExecutorService socketExecutor;
    private final ExecutorService streamExecutor;
    private final static GeneralConfig.SerializationType SERIALIZATION_TYPE = GeneralConfig.getTCPSerializationType();

    /**
     * Command marshaller to unmarshall the received message.
     */
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    public BlockingServerImpl(Role roleInstance) {
        super(roleInstance);
        if (keepAlive) {
            socketExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            streamExecutor = Executors.newCachedThreadPool();
        } else {
            socketExecutor = Executors.newCachedThreadPool();
            streamExecutor = null;
        }
    }

    @Override
    public void shutdown() {
        this.isRunning = false;
        socketExecutor.shutdown();
        Logger.info("Server socketExecutor shutdown [" + socketExecutor.isShutdown() + "], info=[" + socketExecutor + "]");
        if (keepAlive) {
            streamExecutor.shutdown();
            Logger.info("Server streamExecutor shutdown [" + streamExecutor.isShutdown() + "], info=[" + streamExecutor + "]");
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
        serverThread = new Thread(receiver, "server-thread");
        serverThread.start();
    }

    private Runnable createReceiver(final int port) {
        return () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    socket.setKeepAlive(keepAlive);

                    Stack<Closeable> closeables = new Stack<>();
                    closeables.push(serverSocket);

                    InputStream is = collectInputStreams(socket, closeables);

                    SocketHandler socketHandler = new SocketHandler(is, closeables);
                    socketExecutor.execute(socketHandler);
                }
                Logger.info("ServerSocket end!");
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    Logger.warn("Socket might be closed: " + e);
                } else {
                    Logger.error(e);
                }
            }
        };
    }

    private InputStream collectInputStreams(Socket socket, Stack<Closeable> closeables) throws IOException {
        closeables.push(socket);

        InputStream socketInputStream = socket.getInputStream();
        closeables.push(socketInputStream);

        BufferedInputStream bIs = new BufferedInputStream(socketInputStream);
        closeables.push(bIs);
        socketInputStream = bIs;
        switch (SERIALIZATION_TYPE) {
            case OBJECT:
                ObjectInputStream oIs = new ObjectInputStream(socketInputStream);
                closeables.push(oIs);
                return oIs;
            case BYTE:
                DataInputStream dIn = new DataInputStream(socketInputStream);
                closeables.push(dIn);
                return dIn;
            default:
                throw new IOException("serialization type could not be determined!");
        }
    }

    protected class SocketHandler implements Runnable {
        private final InputStream inputStream;
        private final Stack<Closeable> closeables;

        SocketHandler(InputStream inputStream, Stack<Closeable> closeables) {
            this.inputStream = inputStream;
            this.closeables = closeables;
        }

        @Override
        public void run() {
            do {
                handleRead();
            } while (keepAlive && isRunning);
            close();
        }

        public void close() {
            while(!closeables.isEmpty()) {
                Closeable closeable = closeables.pop();
                try {
                    closeable.close();
                } catch (IOException e) {
                    Logger.error(e);
                }
            }
        }

        private void handleRead() {
            try {
                Object message = null;

                synchronized (inputStream) {
                    if (inputStream instanceof ObjectInputStream) {
                        message = ((ObjectInputStream) inputStream).readObject();
                    } else if (inputStream instanceof DataInputStream) {
                        DataInputStream dIn = (DataInputStream) inputStream;
                        int size = dIn.readInt();
                        byte[] msg = new byte[size];
                        final int read = dIn.read(msg);
                        if (read == size) {
                            message = commandMarshaller.unmarshall(msg);
                        } else {
                            throw new IOException(String.format("Read bytes do not match the expected size:[exp=%d,act=%d]!", size, read));
                        }
                    } else {
                        throw new UnsupportedOperationException("input stream not recognized: " + inputStream);
                    }
                }

                if(message != null){
                    NetworkCommand command = (NetworkCommand) message;
                    if (keepAlive) {
                        if (command instanceof SignalEnd_NC) {
                            roleInstance.handleNetworkCommand(command);
                        } else {
                            streamExecutor.execute(() -> roleInstance.handleNetworkCommand(command));
                        }
                    } else {
                        roleInstance.handleNetworkCommand(command);
                    }
                } else {
                    Logger.warn("message received is null!");
                }
            }
            catch (EOFException e) {
//                Logger.warn(e, "");
            } catch (IOException | ClassNotFoundException e) {
                Logger.error(e,"");
            }
        }
    }
}