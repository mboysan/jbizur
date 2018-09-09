package ee.ut.jbizur;

import ee.ut.jbizur.role.bizur.BizurBuilder;
import ee.ut.jbizur.role.bizur.BizurNode;
import mpi.MPIException;

import java.net.UnknownHostException;

/**
 * Assumes a single JVM is running per Node.
 */
public class InitMainMultiJVM {

    public static void main(String[] args) throws UnknownHostException, InterruptedException, MPIException {
        BizurNode node = BizurBuilder.builder().build();
    }
}
