package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.common.Ack_NC;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.role.RoleSettings;
import ee.ut.jbizur.util.IdUtils;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SyncMessageListener {
    private CountDownLatch processesLatch;
    private Map<Class<? extends NetworkCommand>, List<IHandler>> commandHandlers = new HashMap<>();;
    private AtomicInteger ackCount = new AtomicInteger(0);
    private int msgId;
    private int quorumSize;
    private Object info;
    private AtomicReference<Object> passedObjectRef = new AtomicReference<>();

    private SyncMessageListener() {

    }

    public static SyncMessageListener buildWithDefaultHandlers() {
        return build()
                .registerHandler(Ack_NC.class, (c,l) -> {})
                .registerHandler(AckRead_NC.class, (c, l) -> {})
                .registerHandler(AckVote_NC.class, (c, l) -> {})
                .registerHandler(AckWrite_NC.class, (c, l) -> {})
                .registerHandler(Nack_NC.class, (c,l) -> {})
                .registerHandler(NackRead_NC.class, (c, l) -> {})
                .registerHandler(NackVote_NC.class, (c, l) -> {})
                .registerHandler(NackWrite_NC.class, (c, l) -> {});
    }

    public static SyncMessageListener build() {
        return new SyncMessageListener()
                .withMsgId(IdUtils.generateId())
                .withTotalProcessCount(1)
                .withDebugInfo("no_debug_info");
    }

    public SyncMessageListener withMsgId(int msgId) {
        this.msgId = msgId;
        return this;
    }

    public SyncMessageListener withTotalProcessCount(int totalProcessCount) {
        this.processesLatch = new CountDownLatch(totalProcessCount);
        this.quorumSize = RoleSettings.calcQuorumSize(totalProcessCount);
        return this;
    }

    public SyncMessageListener withDebugInfo(Object info) {
        this.info = info;
        return this;
    }

    public SyncMessageListener registerHandler(Class<? extends NetworkCommand> commandClass, IHandler handler) {
        List<IHandler> handlers = commandHandlers.get(commandClass);
        if (handlers == null) {
            handlers = new ArrayList<>();
        }
        handlers.add(handler);
        commandHandlers.put(commandClass, handlers);
        return this;
    }

    public void handleMessage(NetworkCommand command) {
        List<IHandler> handlers = commandHandlers.get(command.getClass());
        if (handlers != null) {
            if (command instanceof Ack_NC) {
                incrementAckCount();
            }

            if (!isMajorityAcked()){
                handlers.forEach(handler -> handler.handle(command, this));
            } else {
                end();
                return;
            }
            processesLatch.countDown();
        }
    }

    public AtomicReference<Object> getPassedObjectRef() {
        return passedObjectRef;
    }

    public int getMsgId() {
        return msgId;
    }

    public void countMsgReceived() {
        processesLatch.countDown();
    }

    public void incrementAckCount(){
        ackCount.getAndIncrement();
    }

    public boolean isMajorityAcked(){
        return ackCount.get() >= quorumSize;
    }

    public void end(){
        while (processesLatch.getCount() > 0) {
            processesLatch.countDown();
        }
    }

    public boolean waitForResponses(long timeout, TimeUnit timeUnit) {
        try {
            if (processesLatch.await(timeout, timeUnit)) {
                return true;
            }
            Logger.warn("timeout when waiting for responses on listener: " + toString());
        } catch (InterruptedException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean waitForResponses() {
        return waitForResponses(Conf.get().network.responseTimeoutSec, TimeUnit.SECONDS);
    }

    @Override
    public String toString() {
        return "SyncMessageListener{" +
                "processesLatch=" + processesLatch.getCount() +
                ", commandHandlers=" + commandHandlers.keySet().stream().map(Class::getSimpleName).collect(Collectors.joining(",")) +
                ", ackCount=" + ackCount +
                ", msgId=" + msgId +
                ", quorumSize=" + quorumSize +
                ", passedObjectRef=" + passedObjectRef +
                ", debugInfo=[" + info + "]" +
                '}';
    }

    public interface IHandler {
        void handle(NetworkCommand command, SyncMessageListener listener);
    }
}
