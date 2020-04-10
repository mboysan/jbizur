package ee.ut.jbizur.protocol.commands.intl;

import ee.ut.jbizur.protocol.commands.net.Nack_NC;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;

public class SendFail_IC extends InternalCommand {
    private final NetworkCommand networkCommand;

    public SendFail_IC(NetworkCommand networkCommand) {
        this.networkCommand = networkCommand;
    }

    public NetworkCommand getNetworkCommand() {
        return networkCommand;
    }

    public Nack_NC getNackNC() {
        return (Nack_NC) new Nack_NC()
                .setSenderId(networkCommand.getSenderId())
                .setSenderAddress(networkCommand.getSenderAddress())
                .setReceiverAddress(networkCommand.getReceiverAddress())
                .setMsgId(networkCommand.getMsgId())
                .setContextId(networkCommand.getContextId());
    }
}
