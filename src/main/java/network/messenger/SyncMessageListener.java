package network.messenger;

import config.GlobalConfig;
import protocol.commands.NetworkCommand;
import role.Role;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SyncMessageListener {

    private final CountDownLatch processesLatch;
    private final AtomicInteger ackCount;
    private final String msgId;

    public SyncMessageListener(String msgId) {
        this.msgId = msgId;
        this.processesLatch = new CountDownLatch(GlobalConfig.getInstance().getProcessCount());
        this.ackCount = new AtomicInteger(0);
    }

    public abstract void handleMessage(NetworkCommand command);

    public String getMsgId() {
        return msgId;
    }

    public CountDownLatch getProcessesLatch() {
        return processesLatch;
    }

    public AtomicInteger getAckCount() {
        return ackCount;
    }

    public boolean isMajorityAcked(){
        return getAckCount().get() >= GlobalConfig.getInstance().getQuorumSize();
    }

    public void end(){
        for (long i = 0; i < getProcessesLatch().getCount(); i++) {
            getProcessesLatch().countDown();
        }
    }

    @Override
    public String toString() {
        return "SyncMessageListener{" +
                "msgId='" + msgId + '\'' +
                '}';
    }
}
