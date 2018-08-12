package ee.ut.jbizur.config;

import mpi.MPI;
import ee.ut.jbizur.network.ConnectionProtocol;
import org.pmw.tinylog.Logger;

import java.util.Arrays;

/**
 * Used to resolve program arguments passed manually, based on the connection type.
 */
public class UserSettings {

    /**
     * Number of tasks to complete. Usually equals to (numNodes * numTasksPerNode)
     */
    private int taskCount = 0;
    /**
     * System monitoring is on/off
     */
    private boolean monitorSystem = false;
    /**
     * Either MPI groupId, or TCP multicast group port.
     */
    private int groupId = -1;
    /**
     * TCP multicast group name/address.
     */
    private String groupName = null;

    public UserSettings(String[] args, ConnectionProtocol connectionProtocol){
        Logger.info("Args received: " + Arrays.toString(args));
        switch (connectionProtocol) {
            case TCP_CONNECTION:
                resolveTCPArgs(args);
                break;
            case MPI_CONNECTION:
                resolveMPIArgs(args);
                break;
        }
        Logger.debug("Args resolved: " + toString());
    }

    private void resolveTCPArgs(String[] args) {
        taskCount = 1;
        monitorSystem = false;
        groupId = 9090;
        groupName = "all-systems.mcast.net";

        if(args != null){
            if(args.length >= 1){
                taskCount = Integer.parseInt(args[0]);
            }
            if(args.length >= 2){
                monitorSystem = Boolean.valueOf(args[1]);
            }
            if(args.length >= 3){
                groupId = Integer.parseInt(args[2]);
            }
            if(args.length >= 4){
                groupName = args[3];
            }
        }
    }

    private void resolveMPIArgs(String[] args) {
        taskCount = 0;
        monitorSystem = false;
        groupId = MPI.ANY_TAG;
        groupName = null;

        if(args != null){
            if(args.length >= 1){
                groupId = Integer.parseInt(args[0]);
            }
            if(args.length >= 1){
                monitorSystem = Boolean.valueOf(args[1]);
            }
        }
    }

    public int getTaskCount() {
        return taskCount;
    }

    public boolean isMonitorSystem() {
        return monitorSystem;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "taskCount=" + taskCount +
                ", monitorSystem=" + monitorSystem +
                ", groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                '}';
    }
}
