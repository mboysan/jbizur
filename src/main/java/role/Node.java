package role;

import config.GlobalConfig;
import network.address.Address;
import network.messenger.SyncMessageListener;
import org.pmw.tinylog.Logger;
import protocol.commands.NetworkCommand;
import protocol.commands.ping.Ping_NC;
import protocol.commands.ping.Pong_NC;
import testframework.TestFramework;
import testframework.result.LatencyResult;

import java.util.concurrent.CountDownLatch;

import static testframework.TestPhase.PHASE_CUSTOM;

/**
 * The main processor (a.k.a process) that sends specified messages and handles the received ones.
 */
public class Node extends Role {
    /**
     * Currently only used to initializes the Pinger node.
     * @param myAddress  address of the node.
     */
    public Node(Address myAddress) throws InterruptedException {
        super(myAddress);
        Logger.info("Node created: " + this.toString());
    }

    /**
     * Sends {@link Ping_NC} request to all processes.
     */
    public void pingAll() {
        String msgId = GlobalConfig.getInstance().generateMsgId(this);

        final CountDownLatch latch = new CountDownLatch(GlobalConfig.getInstance().getProcessCount());
        SyncMessageListener listener = new SyncMessageListener(msgId) {
            @Override
            public void handleMessage(NetworkCommand message) {
                latch.countDown();
            }
        };
        attachMsgListener(listener);

        for (Address receiverAddress : GlobalConfig.getInstance().getAddresses()) {
            NetworkCommand ping = new Ping_NC()
                    .setSenderId(getRoleId())
                    .setReceiverAddress(receiverAddress)
                    .setSenderAddress(getAddress())
                    .setAssocMsgId(msgId);
            sendMessage(ping);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Logger.error(e);
        }
        detachMsgListener(listener);
    }

    /**
     * Sends {@link Pong_NC} response to source process.
     * @param message the ping request received.
     */
    private void pong(Ping_NC message) {
        NetworkCommand pong = new Pong_NC()
                .setSenderId(getRoleId())
                .setReceiverAddress(message.getSenderAddress())
                .setSenderAddress(getAddress())
                .setMsgId(GlobalConfig.getInstance().generateMsgId(this))
                .setAssocMsgId(message.getAssocMsgId());
        sendMessage(pong);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMessage(NetworkCommand message) {
        super.handleMessage(message);
        if (message instanceof Ping_NC) {
            pong((Ping_NC) message);
        }
        if (message instanceof Pong_NC) {
            if(TestFramework.isTesting){
                /* collect latency result and add it to test result collector */
                long currTime = System.currentTimeMillis();
                TestFramework.addLatencyResult(
                        new LatencyResult(
                                "pingSingle",
                                PHASE_CUSTOM,
                                message.getSenderId(),
                                currTime,
                                message.getTimeStamp(),
                                currTime
                        )
                );
            }
        }
    }
}
