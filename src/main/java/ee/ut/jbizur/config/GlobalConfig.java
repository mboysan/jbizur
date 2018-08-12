package ee.ut.jbizur.config;

import ee.ut.jbizur.annotations.ForTestingOnly;
import mpi.MPI;
import mpi.MPIException;
import ee.ut.jbizur.network.ConnectionProtocol;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MPIAddress;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.messenger.Multicaster;
import org.pmw.tinylog.Logger;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.Connect_NC;
import ee.ut.jbizur.role.Role;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ee.ut.jbizur.network.ConnectionProtocol.MPI_CONNECTION;
import static ee.ut.jbizur.network.ConnectionProtocol.TCP_CONNECTION;

/**
 * Global configuration class.
 */
public class GlobalConfig {
    static {
        LoggerConfig.configureLogger();
    }

    /**
     * Singleton instance
     */
    private static GlobalConfig ourInstance = new GlobalConfig();

    /**
     * Timeout (in seconds) for responses between the processes.
     */
    public static long RESPONSE_TIMEOUT_SEC = 5;
    /**
     * Number of times to retry the failing message.
     */
    public static int SEND_FAIL_RETRY_COUNT = 0;

    public static int MAX_ELECTION_WAIT_SEC = 5;

    /**
     * indicates if there are more than one node running on a single JVM. Meaning, if true, the nodes are initiated
     * in a single JVM and tests are done in that JVM. Otherwise, each node is assumed to have its own dedicated JVM.
     */
    private boolean isSingleJVM = false;
    /**
     * List of addresses of the host processes
     */
    private Set<Address> addresses;
    /**
     * Connection ee.ut.jbizur.protocol to use
     */
    private ConnectionProtocol connectionProtocol;
    /**
     * Latch for keeping track of the end cycle.
     */
    private CountDownLatch endLatch;

    private Multicaster multicaster;
    private MulticastAddress multicastAddress;

    /**
     * @return singleton instance, i.e. {@link #ourInstance}
     */
    public static GlobalConfig getInstance() {
        return ourInstance;
    }

    private GlobalConfig() {
        this.addresses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public void initTCP(boolean isSingleJVM){
        initTCP(isSingleJVM, null);
    }

    /**
     * Initializes the system for TCP communication.
     */
    public void initTCP(boolean isSingleJVM, MulticastAddress multicastAddress) {
        this.multicastAddress = multicastAddress;
        init(TCP_CONNECTION, isSingleJVM);
    }

    /**
     * Initializes the system for MPI communication. The MPI addresses of the processes are registered
     * as a side effect.
     * @param args additional arguments for MPI
     * @throws MPIException if MPI could not be initiated
     */
    public void initMPI(String[] args) throws MPIException {
        MPI.Init(args);
        init(MPI_CONNECTION, false);
    }

    /**
     * @param connectionProtocol sets {@link #connectionProtocol}
     */
    private void init(ConnectionProtocol connectionProtocol, boolean isSingleJVM) {
        Logger.info(String.format("Init [%s, isSingleJVM:%s]", connectionProtocol.toString(), isSingleJVM));
        this.connectionProtocol = connectionProtocol;
        this.isSingleJVM = isSingleJVM;
        resetEndLatch(1);   // only 1 receiver per jvm
    }

    /**
     * Registers a {@link Role}. Used when a new node is joined.
     * @param role the ee.ut.jbizur.role to register.
     */
    public void registerRole(Role role){
        Logger.info("Registering ee.ut.jbizur.role: " + role);
        if (connectionProtocol == TCP_CONNECTION){
            if(isSingleJVM) {
                /* No multicasting is needed, just add addresses */
                registerAddress(role.getAddress(), role);
            } else {
                NetworkCommand connect = new Connect_NC()
                        .setSenderAddress(role.getAddress());
                for (int i = 0; i < 5; i++) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Logger.error(e);
                    }
                    Logger.debug("Multicasting nodes: " + connect);
                    getMulticaster(role).multicast(connect);
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Logger.error(e);
                    }
                }
            }
        } else if(connectionProtocol == MPI_CONNECTION){
            /* first register self */
            registerAddress(role.getAddress(), role);
            for (int i = 0; i < getProcessCount(); i++) {
                /* register others. If address is same with self, returns anyways. */
                registerAddress(new MPIAddress(i, ((MPIAddress)role.getAddress()).getGroupId()), role);
            }
        }
    }

    /**
     * If the active connection is TCP connection, creates a multicaster that will send broadcast messages.
     * @param role ee.ut.jbizur.role for handling multicast messages received.
     * @return created multicaster if applicable, null otherwise.
     */
    private Multicaster getMulticaster(Role role){
        if(connectionProtocol == TCP_CONNECTION){
            if(multicaster == null){
                try {
                    multicaster = new Multicaster(multicastAddress, role);
                } catch (IllegalArgumentException e){
                    Logger.error(e);
                }
            }
        }
        return multicaster;
    }

    /**
     * Adds address to the set of address. If address already exist returns without doing anything.
     * @param toRegister address to register
     * @param roleRef    reference to the ee.ut.jbizur.role. Used to modify its properties based on the addresses change.
     */
    public synchronized void registerAddress(Address toRegister, Role roleRef){
        boolean isNew = true;
        for (Address address : addresses) {
            if(address.isSame(toRegister)){
                isNew = false;
                break;
            }
        }
        if(isNew){
            addresses.add(toRegister);
            if(isSingleJVM){
                resetEndLatch(getProcessCount());
            } else {
                resetEndLatch(1);
            }
            Logger.info(String.format("Address [%s] registered on ee.ut.jbizur.role [%s]", toRegister, roleRef));
        }
    }

    public synchronized void unregisterAddress(Address toUnregister, Role roleRef) {
        if(addresses.remove(toUnregister)) {
            if(isSingleJVM){
                resetEndLatch(getProcessCount());
            } else {
                resetEndLatch(1);
            }
            Logger.info(String.format("Address [%s] unregistered from ee.ut.jbizur.role [%s]", toUnregister, roleRef));
        }
    }

    /**
     * Resets the {@link #endLatch} with the given <tt>count</tt>.
     * @param count the count of the existing processes to wait for,
     */
    private synchronized void resetEndLatch(int count){
        Logger.debug("Resetting endLatch with count: " + count);
        endLatch = new CountDownLatch(count);
    }

    /**
     * Signal the end cycle.
     */
    public void readyEnd() {
        Logger.debug(String.format("Entering end cycle [%s]", connectionProtocol));
        if(multicaster != null) {
            multicaster.shutdown();
        }
        endLatch.countDown();
    }

    /**
     * Ends everything.
     * @throws MPIException if MPI could not be finalized
     * @throws InterruptedException in case operations on {@link #endLatch} fails.
     */
    public void end() throws MPIException, InterruptedException {
        Logger.debug("Waiting end cycle with count: " + endLatch.getCount());
        endLatch.await(1, TimeUnit.MINUTES);
        if (connectionProtocol == MPI_CONNECTION) {
            MPI.Finalize();
        }
        Logger.info(String.format("Finalized [%s]", connectionProtocol));
    }

    /**
     * @return gets {@link #addresses}
     */
    public Set<Address> getAddresses() {
        return addresses;
    }

    /**
     * @return the length of the {@link #addresses} array.
     */
    public int getProcessCount(){
        try{
            if(connectionProtocol == MPI_CONNECTION){
                synchronized (MPI.COMM_WORLD) {
                    return MPI.COMM_WORLD.getSize();
                }
            } else if(connectionProtocol == TCP_CONNECTION) {
                return getAddresses().size();
            }
        } catch (Exception e) {
            Logger.error(e, "Could not determine process count");
        }
        return -1;
    }

    public int getQuorumSize() {
        return getProcessCount()/2 + 1;
    }

    public String generateMsgId(){
        return UUID.randomUUID().toString();
    }

    /**
     * @return gets {@link #connectionProtocol}
     */
    public ConnectionProtocol getConnectionProtocol() {
        return connectionProtocol;
    }

    @ForTestingOnly
    public synchronized void reset(){
        getAddresses().clear();
    }
}
