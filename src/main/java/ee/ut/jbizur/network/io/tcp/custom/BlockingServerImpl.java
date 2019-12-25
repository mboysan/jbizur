package ee.ut.jbizur.network.io.tcp.custom;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.AbstractServer;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.SignalEnd_NC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(BlockingServerImpl.class);

    private ServerThread serverThread;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public BlockingServerImpl(String name, TCPAddress tcpAddress) {
        super(name, tcpAddress);
    }

    @Override
    public TCPAddress getServerAddress() {
        return (TCPAddress) super.getServerAddress();
    }

    @Override
    public String toString() {
        return "BlockingServerImpl{}";
    }

    @Override
    public void start() {
        serverThread = new ServerThread(getServerAddress().getPortNumber());
        serverThread.start();
        super.start();
    }

    @Override
    public void close() {
        super.close();
        serverThread.close();
        executor.shutdown();
        try {
            executor.awaitTermination(Conf.get().network.shutdownWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    private class ServerThread extends Thread implements AutoCloseable {
        private ServerSocket serverSocket;

        ServerThread(int port) {
            super("server-thread-" + port);
            try {
                this.serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        @Override
        public void run() {
            try {
                while (isRunning()) {
                    Socket socket = serverSocket.accept();
                    RecvSocket recvSocket = new RecvSocket(socket, true);
                    SocketHandler socketHandler = new SocketHandler(recvSocket);
                    executor.execute(socketHandler);
                }
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    logger.warn("Socket might be closed: {}", e.getMessage());
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void close() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected class SocketHandler implements Runnable {
        private final RecvSocket recvSocket;

        SocketHandler(RecvSocket recvSocket) {
            this.recvSocket = recvSocket;
        }

        @Override
        public void run() {
            do {
                handleRead();
            } while (isRunning());
            recvSocket.close();
        }

        private void handleRead() {
            try {
                NetworkCommand command = recvSocket.recv();
                if (command instanceof SignalEnd_NC) {
                    recv(command);
                } else {
                    recvAsync(command);
                }
            } catch (EOFException e) {
//                Logger.warn(e, "");
            } catch (IOException | ClassNotFoundException e) {
                logger.error(e.getMessage());
            }
        }
    }
}