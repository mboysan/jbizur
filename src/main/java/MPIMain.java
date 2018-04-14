import config.GlobalConfig;
import config.UserSettings;
import mpi.MPI;
import mpi.MPIException;
import network.address.MPIAddress;
import org.pmw.tinylog.Logger;
import role.Node;
import testframework.SystemMonitor;
import testframework.TestFramework;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static network.ConnectionProtocol.MPI_CONNECTION;

/**
 * Ping-Pong test for MPI
 */
public class MPIMain {

    public static void main(String[] args) throws MPIException, InterruptedException {
        long timeStart = System.currentTimeMillis();

        UserSettings settings = new UserSettings(args, MPI_CONNECTION);

        SystemMonitor sysInfo = null;
        TestFramework testFramework = null;

        if(settings.isMonitorSystem()){
            sysInfo = SystemMonitor.collectEvery(500, TimeUnit.MILLISECONDS);;
        }

        GlobalConfig.getInstance().initMPI(args);

        int rank = MPI.COMM_WORLD.getRank();

        int totalProcesses = GlobalConfig.getInstance().getProcessCount();

        Node node = new Node(new MPIAddress(rank, settings.getGroupId()));

        if(node.isLeader()){
            /* start tests */
            testFramework = TestFramework.doPingTests(node, totalProcesses);

            /* send end signal to all nodes */
            node.signalEndToAll();
        }

        GlobalConfig.getInstance().end();

        if(testFramework != null){
            testFramework.printAllOnConsole();
        }
        if(sysInfo != null){
            sysInfo.printOnConsole();
        }

        Logger.info("Total time (ms): " + (System.currentTimeMillis() - timeStart));
    }
}
