package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractServer;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The message receiver wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingServerImpl extends AbstractServer {

    private ServerThread serverThread;

    public BlockingServerImpl(Role roleInstance) {
        super(roleInstance);
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
        serverThread = new ServerThread(tcpAddress.getPortNumber());
        serverThread.start();
    }

    @Override
    public void shutdown() {
        this.isRunning = false;
        serverThread.shutdown();
    }

    private class ServerThread extends Thread {
        private final ExecutorService socketExecutor;
        private final ExecutorService streamExecutor;

        private ServerSocket serverSocket;

        ServerThread(int port) {
            super("server-thread-" + roleInstance.getSettings().getRoleId());

            if (keepAlive) {
                socketExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                streamExecutor = Executors.newCachedThreadPool();
            } else {
                socketExecutor = Executors.newCachedThreadPool();
                streamExecutor = null;
            }

            try {
                this.serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                Logger.error(e);
            }
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    Socket socket = serverSocket.accept();

                    RecvSocket recvSocket = new RecvSocket(socket, keepAlive);
                    SocketHandler socketHandler = new SocketHandler(recvSocket, streamExecutor);
                    socketExecutor.execute(socketHandler);
                }
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    Logger.warn("Socket might be closed: " + e);
                } else {
                    Logger.error(e);
                }
            } finally {
                Logger.info("ServerSocket end!");
            }
        }

        void shutdown() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Logger.error(e);
            }
            socketExecutor.shutdown();
            Logger.info("Server socketExecutor shutdown [" + socketExecutor.isShutdown() + "], info=[" + socketExecutor + "]");
            if (keepAlive) {
                streamExecutor.shutdown();
                Logger.info("Server streamExecutor shutdown [" + streamExecutor.isShutdown() + "], info=[" + streamExecutor + "]");
            }
        }
    }

    protected class SocketHandler implements Runnable {
        private final RecvSocket recvSocket;
        private final ExecutorService streamExecutor;

        SocketHandler(RecvSocket recvSocket, ExecutorService streamExecutor) {
            this.recvSocket = recvSocket;
            this.streamExecutor = streamExecutor;
        }

        @Override
        public void run() {
            do {
                handleRead();
            } while (keepAlive && isRunning);
            recvSocket.close();
        }

        private void handleRead() {
            try {
                NetworkCommand command = recvSocket.recv();
                if (keepAlive) {
                    if (command instanceof SignalEnd_NC) {
                        roleInstance.handleNetworkCommand(command);
                    } else {
                        streamExecutor.execute(() -> roleInstance.handleNetworkCommand(command));
                    }
                } else {
                    roleInstance.handleNetworkCommand(command);
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