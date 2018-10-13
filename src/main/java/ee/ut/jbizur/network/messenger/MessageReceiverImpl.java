package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.config.LoggerConfig;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.network.address.MPIAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;
import mpi.MPI;
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
public class MessageReceiverImpl implements IMessageReceiver {

    private volatile boolean isRunning = true;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final static GeneralConfig.SerializationType SERIALIZATION_TYPE = GeneralConfig.getTCPSerializationType();

    /**
     * The {@link Role} to send the received message for processing.
     */
    private final Role roleInstance;
    /**
     * Command marshaller to unmarshall the received message.
     */
    private final CommandMarshaller commandMarshaller = new CommandMarshaller();

    /**
     * Initializes the message receiver. It then creates the appropriate handler to handle the received message.
     *
     * @param roleInstance sets {@link #roleInstance}
     */
    public MessageReceiverImpl(Role roleInstance) {
        this.roleInstance = roleInstance;
    }

    @Override
    public void shutdown() {
        this.isRunning = false;
        executor.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRecv(){
        switch (NodeConfig.getConnectionProtocol()) {
            case TCP:
                new TCPReceiver().start();
                break;
            case MPI:
                new MPIReceiver().start();
                break;
        }
    }

    /**
     * TCP recv handler
     */
    private class TCPReceiver extends Thread {
        @Override
        public void run() {
            runOnTCP();
            if (LoggerConfig.isDebugEnabled()) {
                Logger.debug("receiver end");
            }
        }

        /**
         * Recv message with TCP
         */
        private void runOnTCP() {
            ServerSocket serverSocket;
            Socket socket = null;
            try {
                TCPAddress prevAddr = (TCPAddress) roleInstance.getSettings().getAddress();
                serverSocket = new ServerSocket(prevAddr.getPortNumber());

                TCPAddress modifiedAddr = new TCPAddress(prevAddr.getIp(), serverSocket.getLocalPort());
                roleInstance.setAddress(modifiedAddr);
                roleInstance.setReady();

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
        }
    }

    /**
     * MPI recv handler
     */
    private class MPIReceiver extends Thread {
        @Override
        public void run() {
            runOnMPI();
        }

        /**
         * Recv message with MPI
         */
        private void runOnMPI(){
            try {
                roleInstance.setReady();
                MPIAddress roleAddress = (MPIAddress) roleInstance.getSettings().getAddress();
                while (isRunning){
                    int[] msgInfo = new int[1];
//                  MPI.COMM_WORLD.recv(msgInfo, msgInfo.length, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);   // receive msg length first.
                    msgInfo[0] = 512;
                    byte[] msg = new byte[msgInfo[0]];
                    MPI.COMM_WORLD.recv(msg, msg.length, MPI.BYTE, MPI.ANY_SOURCE, roleAddress.getGroupId());

                    NetworkCommand message = commandMarshaller.unmarshall(msg);
                    executor.execute(() -> {
                        roleInstance.handleNetworkCommand(message);
                    });
                }
            } catch (Exception e) {
                Logger.error(e, "Recv err");
            }
        }
    }
}