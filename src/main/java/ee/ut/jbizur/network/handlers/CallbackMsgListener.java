package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.ICommand;

import java.util.function.Predicate;

public class CallbackMsgListener extends QuorumBasedMsgListener {

    public CallbackMsgListener(int msgId, MsgListeners msgListeners) {
        this(1, msgId, msgListeners);
    }

    public CallbackMsgListener(int totalProcessCount, int msgId, MsgListeners msgListeners) {
        super(totalProcessCount, totalProcessCount, msgId, msgListeners);
    }

    @Override
    public CallbackMsgListener setHandler(Predicate<ICommand> handler) {
        return (CallbackMsgListener) super.setHandler(handler);
    }

    @Override
    public CallbackMsgListener setCountdownHandler(Predicate<ICommand> countdownHandler) {
        return (CallbackMsgListener) super.setCountdownHandler(countdownHandler);
    }

    @Override
    public <T extends IMsgState> T getState() {
        return (T) new CallbackState(this::awaitResponses);
    }
}
