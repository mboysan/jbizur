package role;

import config.GlobalConfig;
import network.address.Address;
import network.messenger.SyncMessageListener;
import protocol.commands.NetworkCommand;
import protocol.commands.ping.Ping_NC;
import protocol.commands.ping.Pong_NC;
import protocol.internal.InternalCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Pinger extends Role {
    protected final long RETRY_PING_TIMEOUT_SEC = 5;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<Address, Integer> pingMap = new ConcurrentHashMap<>();

    public Pinger(Role rootRole) throws InterruptedException {
        super(rootRole);
    }

    public void pingUnreachableNodesPeriodically() {
        executor.scheduleAtFixedRate(() -> {
            pingMap.forEach((address, i) -> {
                if(isNodeReachable(address)) {
                    pingMap.remove(address);
                    GlobalConfig.getInstance().registerAddress(address, rootRole);
                } else {
                    int retryCount = pingMap.get(address) + 1;
                    if(retryCount == 5) {
                        rootRole.handleNodeFailure(address);
                    }
                    pingMap.put(address, retryCount);
                }
            });
        },0, RETRY_PING_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    protected void registerUnreachableAddress(Address nodeAddress) {
        pingMap.putIfAbsent(nodeAddress, 0);
    }

    protected void ping(Address receiverAddress) {
        ping(receiverAddress, null);
    }

    protected void ping(Address receiverAddress, String msgId) {
        NetworkCommand ping = new Ping_NC()
                .setSenderId(getRoleId())
                .setReceiverAddress(receiverAddress)
                .setSenderAddress(getAddress())
                .setMsgId(msgId);
        sendMessage(ping);
    }

    /**
     * Sends {@link Pong_NC} response to source process.
     *
     * @param pingNc the ping request received.
     */
    private void pong(Ping_NC pingNc) {
        NetworkCommand pong = new Pong_NC()
                .setSenderId(getRoleId())
                .setReceiverAddress(pingNc.getSenderAddress())
                .setSenderAddress(getAddress());
        sendMessage(pong);
    }

    protected boolean isNodeReachable(Address nodeAddress) {
        String msgId = GlobalConfig.getInstance().generateMsgId();
        SyncMessageListener listener = new SyncMessageListener(msgId) {
            @Override
            public void handleMessage(NetworkCommand message) {
                if (message instanceof Pong_NC) {
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);
        try {
            ping(nodeAddress, msgId);
            return listener.waitForResponses();
        } finally {
            detachMsgListener(listener);
        }
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {
        //do nothing
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        super.handleNetworkCommand(command);

        if (command instanceof Ping_NC) {
            pong((Ping_NC) command);
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
