package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.RoleSettings;
import org.pmw.tinylog.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SyncMessageListener {
    private final CountDownLatch processesLatch;
    private final AtomicInteger ackCount;
    private final String msgId;
    private final int quorumSize;

    protected SyncMessageListener(String msgId, int totalProcessCount){
        this.msgId = msgId;
        this.processesLatch = new CountDownLatch(totalProcessCount);
        this.ackCount = new AtomicInteger(0);
        this.quorumSize = RoleSettings.calcQuorumSize(totalProcessCount);
    }

    public abstract void handleMessage(NetworkCommand command);

    public String getMsgId() {
        return msgId;
    }

    protected CountDownLatch getProcessesLatch() {
        return processesLatch;
    }

    public void incrementAckCount(){
        ackCount.getAndIncrement();
    }

    public boolean isMajorityAcked(){
        return ackCount.get() >= quorumSize;
    }

    public void end(){
        for (long i = 0; i < getProcessesLatch().getCount(); i++) {
            getProcessesLatch().countDown();
        }
    }

    public boolean waitForResponses(long timeout, TimeUnit timeUnit) {
        try {
            return getProcessesLatch().await(timeout, timeUnit);
        } catch (InterruptedException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean waitForResponses() {
        return waitForResponses(NodeConfig.getResponseTimeoutSec(), TimeUnit.SECONDS);
    }

    @Override
    public String toString() {
        return "SyncMessageListener{" +
                "msgId='" + msgId + '\'' +
                '}';
    }
}
