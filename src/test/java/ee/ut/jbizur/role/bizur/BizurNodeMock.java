package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.network.io.NetworkManagerMock;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.common.Nack_NC;
import org.pmw.tinylog.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class BizurNodeMock extends BizurNode {

    public boolean isDead = false;
    public AtomicInteger hashIndex = new AtomicInteger(-1);

    protected BizurNodeMock(BizurSettings bizurSettings) {
        super(bizurSettings);
    }

    @Override
    protected void initRole() {
        this.networkManager = new NetworkManagerMock(this).start();
    }

    @Override
    protected BucketContainer createBucketContainer() {
        return new BucketContainer(Conf.get().consensus.bizur.bucketCount) {
            @Override
            public int hashKey(String s) {
                int idx = hashIndex.get();
                return idx < 0
                        ? super.hashKey(s)
                        : idx;
            }
        };
    }

    public void kill() {
        isDead = true;
        Logger.info(logMsg("NODE KILLED"));
    }

    public void revive() {
        isDead = false;
        Logger.info(logMsg("NODE REVIVED"));
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        if(isDead) {
            super.handleNetworkCommand(new Nack_NC());
        } else {
            super.handleNetworkCommand(command);
        }
    }

    public NetworkManagerMock getNetworkManager() {
        return (NetworkManagerMock) networkManager;
    }
}
