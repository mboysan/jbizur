package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.protocol.commands.ICommand;
import ee.ut.jbizur.protocol.commands.nc.common.Ack_NC;
import ee.ut.jbizur.protocol.commands.nc.common.Nack_NC;
import org.pmw.tinylog.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class QuorumBasedMsgListener implements IMsgListener {
    private Predicate<ICommand> handler;
    private Predicate<ICommand> countdownHandler;

    private final AtomicInteger ackCount = new AtomicInteger(0);
    private final int quorumSize;
    private final CountDownLatch latch;
    private final int msgId;
    private final MsgListeners msgListeners;

    public QuorumBasedMsgListener(
            int totalProcesses,
            int quorumSize,
            int msgId,
            MsgListeners msgListeners) {
        this.quorumSize = quorumSize;
        this.latch = new CountDownLatch(totalProcesses);
        this.msgId = msgId;
        this.msgListeners = msgListeners;

        this.handler = defaultHandler();
        this.countdownHandler = defaultCountdownHandler();
        if (countdownHandler == null || handler == null) {
            throw new IllegalArgumentException("handlers cannot be null");
        }

        registerSelf(msgListeners);
    }

    protected Predicate<ICommand> defaultHandler() {
        return c -> false;
    }

    protected Predicate<ICommand> defaultCountdownHandler() {
        return c -> {
            if (c instanceof Ack_NC) {
                ackCount.incrementAndGet();
                return true;
            }
            else if (c instanceof Nack_NC) {
                return true;
            }
            return false;
        };
    }

    public QuorumBasedMsgListener setHandler(Predicate<ICommand> handler) {
        if (handler != null) {
            this.handler = handler;
        }
        return this;
    }

    public QuorumBasedMsgListener setCountdownHandler(Predicate<ICommand> countdownHandler) {
        if (countdownHandler != null) {
            this.countdownHandler = countdownHandler;
        }
        return this;
    }

    @Override
    public boolean handle(ICommand command) {
        try {
            boolean isHandled;
            if (isHandled = countdownHandler.test(command)) {
                latch.countDown();
            }
            isHandled = isHandled | handler.test(command);
            if (isHandled && isMajorityAcked()) {
                do {
                    latch.countDown();
                } while (latch.getCount() > 0);
            }
            return isHandled;
        } catch (Exception e) {
            deregisterSelf(msgListeners);
            throw e;
        }
    }

    private boolean isMajorityAcked() {
        return ackCount.get() >= quorumSize;
    }

    boolean awaitResponses() {
        try {
            if (latch.await(Conf.get().network.responseTimeoutSec, TimeUnit.SECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            Logger.error(e);
        } finally {
            deregisterSelf(msgListeners);
        }
        return false;
    }

    boolean awaitMajority() {
        return awaitResponses() && isMajorityAcked();
    }

    @Override
    public Integer getMsgId() {
        return msgId;
    }

    @Override
    public <T extends IMsgState> T getState() {
        return (T) new QuorumState(this::awaitMajority);
    }
}
