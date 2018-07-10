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
    protected static long RETRY_PING_TIMEOUT_SEC = 5;
    protected static long RETRY_PING_COUNT = 5;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<String, Address> strAddrToAddrMap = new ConcurrentHashMap<>();
    private final Map<Address, Integer> addressRetryMap = new ConcurrentHashMap<>();

    public Pinger(Role rootRole) throws InterruptedException {
        super(rootRole);
    }

    public void pingUnreachableNodesPeriodically() {
        executor.scheduleAtFixedRate(() -> {
            addressRetryMap.forEach((address, i) -> {
                if(isNodeReachable(address)) {
                    addressRetryMap.remove(address);
                    GlobalConfig.getInstance().registerAddress(address, rootRole);
                } else {
                    int retryCount = addressRetryMap.get(address) + 1;
                    if(retryCount == RETRY_PING_COUNT) {
                        rootRole.handleNodeFailure(address);
                    }
                    addressRetryMap.put(address, retryCount);
                }
            });
        },0, RETRY_PING_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    protected void registerUnreachableAddress(Address nodeAddress) {
        if(strAddrToAddrMap.putIfAbsent(nodeAddress.toString(), nodeAddress) == null) {
            addressRetryMap.putIfAbsent(nodeAddress, 0);
        }
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
