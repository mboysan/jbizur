import config.GlobalConfig;
import mpi.MPI;
import mpi.MPIException;
import network.address.MPIAddress;
import org.pmw.tinylog.Logger;
import role.Node;
import testframework.SystemMonitor;
import testframework.TestFramework;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Ping-Pong test for MPI
 */
public class MPIMain {

    public static void main(String[] args) throws MPIException, InterruptedException {
        long timeStart = System.currentTimeMillis();

        Logger.info("Args received: " + Arrays.toString(args));
        int groupId = MPI.ANY_TAG;
        boolean monitorSystem = false;
        if(args != null){
            if(args.length >= 1){
                groupId = Integer.parseInt(args[0]);
            }
            if(args.length >= 2){
                monitorSystem = Boolean.valueOf(args[1]);
            }
        }

        SystemMonitor sysInfo = null;
        TestFramework testFramework = null;

        if(monitorSystem){
            sysInfo = SystemMonitor.collectEvery(500, TimeUnit.MILLISECONDS);;
        }

        GlobalConfig.getInstance().initMPI(args);

        int rank = MPI.COMM_WORLD.getRank();

        int totalProcesses = GlobalConfig.getInstance().getProcessCount();

        Node node = new Node(new MPIAddress(rank, groupId));

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
