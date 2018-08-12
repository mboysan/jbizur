package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.config.GlobalConfig;
import org.pmw.tinylog.Logger;
import ee.ut.jbizur.protocol.commands.NetworkCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SyncMessageListener {

    private final CountDownLatch processesLatch;
    private final AtomicInteger ackCount;
    private final String msgId;

    protected SyncMessageListener(String msgId) {
        this(msgId, GlobalConfig.getInstance().getProcessCount());
    }

    protected SyncMessageListener(String msgId, int latchCount){
        this.msgId = msgId;
        this.processesLatch = new CountDownLatch(latchCount);
        this.ackCount = new AtomicInteger(0);
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
        return ackCount.get() >= GlobalConfig.getInstance().getQuorumSize();
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
        return waitForResponses(GlobalConfig.RESPONSE_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    @Override
    public String toString() {
        return "SyncMessageListener{" +
                "msgId='" + msgId + '\'' +
                '}';
    }
}
