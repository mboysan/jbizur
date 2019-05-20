package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.ping.ConnectOK_NC;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import ee.ut.jbizur.protocol.internal.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.internal.NodeAddressUnregistered_IC;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BizurClient extends BizurNode {

    private Address[] addresses;
    private final Object addressesLock = new Object();

    protected BizurClient(BizurSettings bizurSettings) {
        super(bizurSettings);
        if (isAddressesAlreadyRegistered()) {
            arrangeAddresses();
        }
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
            super.handleNetworkCommand(command);
        }
    }

    @Override
    public String get(String key) {
        checkReady();
        ClientResponse_NC response = routeRequestAndGet(
                new ClientApiGet_NC()
                    .setKey(key)
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(getLeaderAddress(key))
                    .setSenderAddress(getSettings().getAddress())
        );
        updateLeaderOfBucket(key, response.getAssumedLeaderAddress());
        return (String) response.getPayload();
    }

    @Override
    public boolean set(String key, String val) {
        checkReady();
        ClientResponse_NC response = routeRequestAndGet(
                new ClientApiSet_NC()
                        .setKey(key)
                        .setVal(val)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress())
        );
        updateLeaderOfBucket(key, response.getAssumedLeaderAddress());
        return (boolean) response.getPayload();
    }

    @Override
    public boolean delete(String key) {
        checkReady();
        ClientResponse_NC response =  routeRequestAndGet(
                new ClientApiDelete_NC()
                        .setKey(key)
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress())
        );
        updateLeaderOfBucket(key, response.getAssumedLeaderAddress());
        return (boolean) response.getPayload();
    }

    @Override
    public Set<String> iterateKeys() {
        checkReady();
        ClientResponse_NC response = routeRequestAndGet(
                new ClientApiIterKeys_NC()
                        .setSenderId(getSettings().getRoleId())
                        .setReceiverAddress(getRandomAddress())
                        .setSenderAddress(getSettings().getAddress())
        );
        return (Set<String>) response.getPayload();
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

    private Address getLeaderAddress(String bucketKey) {
        Address leader = bucketContainer.getBucket(bucketKey).getLeaderAddress();
        return leader != null ? leader : getRandomAddress();
    }

    private void updateLeaderOfBucket(String bucketKey, Address assumedLeader) {
        bucketContainer.getBucket(bucketKey).setLeaderAddress(assumedLeader, false);
    }
}
