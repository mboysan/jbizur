package network.messenger;

import config.GlobalConfig;
import mpi.MPI;
import network.address.MPIAddress;
import network.address.TCPAddress;
import org.pmw.tinylog.Logger;
import protocol.CommandMarshaller;
import protocol.commands.NetworkCommand;
import protocol.commands.ping.SignalEnd_NC;
import role.Role;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The message receiver wrapper for the communication protocols defined in {@link network.ConnectionProtocol}.
 */
public class MessageReceiver {

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
    public MessageReceiver(Role roleInstance) {
        this.roleInstance = roleInstance;

        switch (GlobalConfig.getInstance().getConnectionProtocol()) {
            case TCP_CONNECTION:
                new TCPReceiver().start();
                break;
            case MPI_CONNECTION:
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
            Logger.debug("receiver end");
        }

        /**
         * Recv message with TCP
         */
        private void runOnTCP() {
            ServerSocket serverSocket;
            Socket socket = null;
            try {
                TCPAddress prevAddr = (TCPAddress) roleInstance.getAddress();
                serverSocket = new ServerSocket(prevAddr.getPortNumber());

                TCPAddress modifiedAddr = new TCPAddress(prevAddr.getIp(), serverSocket.getLocalPort());
                roleInstance.setAddress(modifiedAddr);
                roleInstance.setReady();

                while (true) {
                    socket = serverSocket.accept();
                    DataInputStream dIn = new DataInputStream(socket.getInputStream());
                    /* following commented code reads length first
                    int length = dIn.readInt(); // read length of incoming message
                    byte[] msg = null;
                    if(length>0) {
                        msg = new byte[length];
                        dIn.readFully(msg, 0, msg.length); // read the message
                    }
                    */
                    byte[] msg = new byte[512];    //fixed size byte[]
                    dIn.read(msg);
                    if(msg != null){
                        NetworkCommand message = commandMarshaller.unmarshall(new String(msg, StandardCharsets.UTF_8));
                        if(message != null){
                            if(message instanceof SignalEnd_NC){
                                Logger.debug("End signal recv: " + message);
                                executor.shutdown();
                                GlobalConfig.getInstance().readyEnd();
                                break;
                            }
                            executor.execute(() -> {
                                roleInstance.handleMessage(message);
                            });
                        }
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
                MPIAddress roleAddress = (MPIAddress) roleInstance.getAddress();
                while (true){
                    int[] msgInfo = new int[1];
//                  MPI.COMM_WORLD.recv(msgInfo, msgInfo.length, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);   // receive msg length first.
                    msgInfo[0] = 512;
                    byte[] msg = new byte[msgInfo[0]];
                    MPI.COMM_WORLD.recv(msg, msg.length, MPI.BYTE, MPI.ANY_SOURCE, roleAddress.getGroupId());

                    NetworkCommand message = commandMarshaller.unmarshall(new String(msg, StandardCharsets.UTF_8));
                    if(message instanceof SignalEnd_NC){
                        Logger.debug("End signal recv: " + message);
                        executor.shutdown();
                        GlobalConfig.getInstance().readyEnd();
                        break;
                    }
                    executor.execute(() -> {
                        roleInstance.handleMessage(message);
                    });
                }
            } catch (Exception e) {
                Logger.error(e, "Recv err");
            }
        }
    }
}