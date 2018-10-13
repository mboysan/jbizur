package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.network.messenger.Multicaster;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.ClientApiDelete_NC;
import ee.ut.jbizur.protocol.commands.bizur.ClientApiGet_NC;
import ee.ut.jbizur.protocol.commands.bizur.ClientApiIterKeys_NC;
import ee.ut.jbizur.protocol.commands.bizur.ClientApiSet_NC;
import ee.ut.jbizur.protocol.commands.ping.ConnectOK_NC;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import ee.ut.jbizur.protocol.internal.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.internal.NodeAddressUnregistered_IC;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class BizurClient extends BizurNode {

    private Address[] addresses;
    private final Object addressesLock = new Object();

    protected BizurClient(BizurSettings bizurSettings) throws InterruptedException {
        this(bizurSettings, null, null, null, null);
    }

    protected BizurClient(BizurSettings bizurSettings, Multicaster multicaster, IMessageSender messageSender, IMessageReceiver messageReceiver, CountDownLatch readyLatch) throws InterruptedException {
        super(bizurSettings, multicaster, messageSender, messageReceiver, readyLatch);

        if (isAddressesAlreadyRegistered()) {
            arrangeAddresses();
        }
    }

    @Override
    protected void initBuckets() {
    }

    @Override
    protected boolean initLeaderPerBucketElectionFlow() {
        return true;
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {
        super.handleInternalCommand(command);
        if (command instanceof NodeAddressRegistered_IC) {
            arrangeAddresses();
        }
        if (command instanceof NodeAddressUnregistered_IC) {
            arrangeAddresses();
        }
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        Integer assocMsgId = command.getMsgId();
        if(assocMsgId != null){
            SyncMessageListener listener = syncMessageListeners.get(assocMsgId);
            if(listener != null){
                listener.handleMessage(command);
            }
        }

        if(command instanceof ConnectOK_NC){
            getSettings().registerAddress(command.getSenderAddress());
        }
        if (command instanceof SignalEnd_NC) {
            shutdown();
        }
    }

    @Override
    public String get(String key) {
        checkReady();
        return routeRequestAndGet(
                new ClientApiGet_NC()
                        .setKey(key)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress()));
    }

    @Override
    public boolean set(String key, String val) {
        checkReady();
        return routeRequestAndGet(
                new ClientApiSet_NC()
                        .setKey(key)
                        .setVal(val)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress()));
    }

    @Override
    public boolean delete(String key) {
        checkReady();
        return routeRequestAndGet(
                new ClientApiDelete_NC()
                        .setKey(key)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress()));
    }

    @Override
    public Set<String> iterateKeys() {
        checkReady();
        return routeRequestAndGet(
                new ClientApiIterKeys_NC()
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress()));
    }

    @Override
    public void signalEndToAll() {
        super.signalEndToAll();
        handleNetworkCommand(new SignalEnd_NC());
    }

    private void arrangeAddresses() {
        synchronized (addressesLock) {
            addresses = convertAddressSetToArray();
        }
    }

    private Address[] convertAddressSetToArray() {
        Set<Address> addressSet = getSettings().getMemberAddresses();
        Address[] addresses = new Address[addressSet.size()];
        int i = 0;
        for (Address address : addressSet) {
            addresses[i++] = address;
        }
        return addresses;
    }

    public Address getRandomAddress() {
        synchronized (addressesLock) {
            int index = ThreadLocalRandom.current().nextInt(addresses.length);
            return addresses[index];
        }
    }
}
