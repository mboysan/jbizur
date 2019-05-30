package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MsgListeners {

    private final Map<Integer, IMsgListener> msgListenerMap = new ConcurrentHashMap<>();

    public MsgListeners() {}

    public void addMsgListener(int msgId, IMsgListener msgListener) {
        msgListenerMap.put(msgId, msgListener);
    }

    public void removeMsgListener(int msgId) {
        msgListenerMap.remove(msgId);
    }

    private IMsgListener getListener(int msgId) {
        return msgListenerMap.get(msgId);
    }

    public boolean tryHandle(NetworkCommand command) {
        if (command.getMsgId() == null) {
            return false;
        }
        IMsgListener msgListener = getListener(command.getMsgId());
        if (msgListener != null) {
            return msgListener.handle(command);
        }
        return false;
    }

}
