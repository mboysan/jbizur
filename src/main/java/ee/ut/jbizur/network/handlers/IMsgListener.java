package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.ICommand;

public interface IMsgListener {
    boolean handle(ICommand command);

    default void registerSelf(MsgListeners msgListeners) {
        msgListeners.addMsgListener(getMsgId(), this);
    }

    default void deregisterSelf(MsgListeners msgListeners) {
        msgListeners.removeMsgListener(getMsgId());
    }

    Integer getMsgId();

    <T extends IMsgState> T getState();
}
