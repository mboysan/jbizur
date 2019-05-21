package ee.ut.jbizur.network.messenger.tcp.custom;

import ee.ut.jbizur.config.Conf;
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
import java.util.concurrent.TimeUnit;

/**
 * The message receiver wrapper for the communication protocols defined in {@link ee.ut.jbizur.network.ConnectionProtocol}.
 */
public class BlockingServerImpl extends AbstractServer {

    private ServerThread serverThread;

    public BlockingServerImpl(Role roleInstance) {
        super(roleInstance);
        if (!super.keepAlive) {
            throw new UnsupportedOperationException("non keepalive connection type not supported");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRecv(Address address) {
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
        Logger.info("Server shutdown: " + roleInstance.toString());
    }

    private class ServerThread extends Thread {
        private final ExecutorService socketExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

        private ServerSocket serverSocket;

        ServerThread(int port) {
            super("server-thread-" + roleInstance.getSettings().getRoleId());
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

                    RecvSocket recvSocket = new RecvSocket(socket, true);
                    SocketHandler socketHandler = new SocketHandler(recvSocket, streamExecutor);
                    socketExecutor.execute(socketHandler);
                }
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    Logger.warn("Socket might be closed: " + e);
                } else {
                    Logger.error(e);
                }
            }
        }

        void shutdown() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Logger.error(e);
            }

            socketExecutor.shutdown();
            try {
                socketExecutor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Logger.error(e);
            }

            streamExecutor.shutdown();
            try {
                streamExecutor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Logger.error(e);
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
            } while (isRunning);
            recvSocket.close();
        }

        private void handleRead() {
            try {
                NetworkCommand command = recvSocket.recv();
                if (command instanceof SignalEnd_NC) {
                    roleInstance.handleNetworkCommand(command);
                } else {
                    streamExecutor.execute(() -> roleInstance.handleNetworkCommand(command));
                }
            } catch (EOFException e) {
//                Logger.warn(e, "");
            } catch (IOException | ClassNotFoundException e) {
                Logger.error(e, "");
            }
        }
    }
}