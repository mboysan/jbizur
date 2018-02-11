package network;

import protocol.commands.NetworkCommand;
import protocol.commands.internal.InternalCommand;

public interface INetworkOperator extends Runnable{
    void notifyOperator(NetworkCommand networkCommand, Object notifiedFrom);
    void notifyOperator(InternalCommand internalCommand, Object notifiedFrom);
    void init() throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
}