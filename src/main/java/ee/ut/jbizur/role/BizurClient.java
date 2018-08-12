package ee.ut.jbizur.role;

import ee.ut.jbizur.config.GlobalConfig;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.protocol.commands.ping.ConnectOK_NC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BizurClient extends BizurNode {

    private Address[] addresses;

    public BizurClient(Address baseAddress) throws InterruptedException {
        this(baseAddress, null, null, null);
    }

    public BizurClient(Address baseAddress, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        super(baseAddress, messageSender, messageReceiver, readyLatch);
        addresses = convertAddressSetToArray();
    }

    @Override
    protected void initNode() {
        GlobalConfig.getInstance().unregisterAddress(getAddress(), this);
    }

    @Override
    protected void initBuckets(int count) {
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        String assocMsgId = command.getMsgId();
        if(assocMsgId != null){
            SyncMessageListener listener = syncMessageListeners.get(assocMsgId);
            if(listener != null){
                listener.handleMessage(command);
            }
        }

        if(command instanceof ConnectOK_NC){
            GlobalConfig.getInstance().registerAddress(command.getSenderAddress(), this);
            addresses = convertAddressSetToArray();
        }
    }

    @Override
    public String get(String key) {
        return routeRequestAndGet(
                new ClientApiGet_NC()
                        .setKey(key)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getAddress()));
    }

    @Override
    public boolean set(String key, String val) {
        return routeRequestAndGet(
                new ClientApiSet_NC()
                        .setKey(key)
                        .setVal(val)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getAddress()));
    }

    @Override
    public boolean delete(String key) {
        return routeRequestAndGet(
                new ClientApiDelete_NC()
                        .setKey(key)
                        .setSenderId(getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getAddress()));
    }

    @Override
    public Set<String> iterateKeys() {
        return routeRequestAndGet(
                new ClientApiIterKeys_NC()
                        .setSenderId(getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getAddress()));
    }

    @Override
    protected <T> T routeRequestAndGet(NetworkCommand command, int retryCount) throws OperationFailedError {
        if (retryCount < 0) {
            throw new OperationFailedError("Routing failed for command: " + command);
        }
        final Object[] resp = new Object[1];
        String msgId = GlobalConfig.getInstance().generateMsgId();
        SyncMessageListener listener = new SyncMessageListener(msgId, 1) {
            @Override
            public void handleMessage(NetworkCommand command) {
                if(command instanceof Nack_NC) {
                    resp[0] = new SendFail_IC(command);
                    getProcessesLatch().countDown();
                } else if (command instanceof LeaderResponse_NC){
                    resp[0] = command.getPayload();
                    getProcessesLatch().countDown();
                }
            }
        };
        attachMsgListener(listener);
        try {
            command.setMsgId(msgId);
            sendMessage(command);

            long waitSec = addresses.length * GlobalConfig.MAX_ELECTION_WAIT_SEC + GlobalConfig.RESPONSE_TIMEOUT_SEC;
            if (listener.waitForResponses(waitSec, TimeUnit.SECONDS)) {
                T rsp = (T) resp[0];
                if(!(rsp instanceof SendFail_IC)) {
                    return rsp;
                }
            }

            return routeRequestAndGet(command, retryCount-1);

        } finally {
            detachMsgListener(listener);
        }
    }

    @Override
    public void signalEndToAll() {
        GlobalConfig.getInstance().registerAddress(getAddress(), this);
        super.signalEndToAll();
    }

    private Address[] convertAddressSetToArray() {
        Set<Address> addressSet = GlobalConfig.getInstance().getAddresses();
        Address[] addresses = new Address[addressSet.size()];
        int i = 0;
        for (Address address : addressSet) {
            addresses[i++] = address;
        }
        return addresses;
    }

    public Address getRandomAddress() {
        int index = ThreadLocalRandom.current().nextInt(addresses.length);
        return addresses[index];
    }
}
