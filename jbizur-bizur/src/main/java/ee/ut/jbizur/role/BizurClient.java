package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.InternalCommand;
import ee.ut.jbizur.protocol.commands.intl.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.commands.intl.NodeAddressUnregistered_IC;
import ee.ut.jbizur.protocol.commands.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BizurClient extends BizurNode {

    private static final Logger logger = LoggerFactory.getLogger(BizurClient.class);

    private Address[] addresses;
    private final Object addressesLock = new Object();

    protected BizurClient(BizurSettings bizurSettings) throws IOException {
        super(bizurSettings);
        if (isAddressesAlreadyRegistered()) {
            arrangeAddresses();
        }
    }

    @Override
    protected void handle(InternalCommand ic) {
        super.handle(ic);
        if (ic instanceof NodeAddressRegistered_IC) {
            arrangeAddresses();
        }
        if (ic instanceof NodeAddressUnregistered_IC) {
            arrangeAddresses();
        }
    }

    @Override
    public void handle(NetworkCommand command) {
        if(command instanceof ConnectOK_NC){
            getSettings().registerAddress(command.getSenderAddress());
        }
        if (command instanceof SignalEnd_NC) {
            super.handle(command);
        }
    }

    @Override
    public String get(String key) {
        checkReady();
        ClientResponse_NC response = null;
        try {
            response = route(
                    new ClientApiGet_NC()
                        .setKey(key)
                        .setReceiverAddress(getLeaderAddress(key))
                        .setSenderAddress(getSettings().getAddress())
                        .setCorrelationId(IdUtil.generateId())
            );
            updateLeaderOfBucket(key, response.getAssumedLeaderAddress());
            return (String) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean set(String key, String val) {
        checkReady();
        ClientResponse_NC response = null;
        try {
            response = route(
                    new ClientApiSet_NC()
                            .setKey(key)
                            .setVal(val)
                            .setReceiverAddress(getLeaderAddress(key))
                            .setSenderAddress(getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            updateLeaderOfBucket(key, response.getAssumedLeaderAddress());
            return (boolean) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(String key) {
        checkReady();
        ClientResponse_NC response = null;
        try {
            response = route(
                    new ClientApiDelete_NC()
                            .setKey(key)
                            .setReceiverAddress(getLeaderAddress(key))
                            .setSenderAddress(getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            updateLeaderOfBucket(key, response.getAssumedLeaderAddress());
            return (boolean) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> iterateKeys() {
        checkReady();
        ClientResponse_NC response = null;
        try {
            response = route(
                    new ClientApiIterKeys_NC()
                            .setReceiverAddress(getRandomAddress())
                            .setSenderAddress(getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            return (Set<String>) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
        int index = bucketContainer.hashKey(bucketKey);
        Bucket bucket = bucketContainer.lockAndGetBucket(index);
        try {
            Address lead = bucket.getLeaderAddress();
            return lead != null ? lead : getRandomAddress();
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }

    private void updateLeaderOfBucket(String bucketKey, Address assumedLeader) {
        int index = bucketContainer.hashKey(bucketKey);
        Bucket bucket = bucketContainer.lockAndGetBucket(index);
        try {
            bucket.setLeaderAddress(assumedLeader);
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }
}
