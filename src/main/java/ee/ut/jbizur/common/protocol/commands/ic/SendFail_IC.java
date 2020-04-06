package ee.ut.jbizur.common.protocol.commands.ic;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.common.protocol.commands.nc.common.Nack_NC;

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
