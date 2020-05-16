package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.RoundRobin;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.InternalCommand;
import ee.ut.jbizur.protocol.commands.intl.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.commands.intl.NodeAddressUnregistered_IC;
import ee.ut.jbizur.protocol.commands.net.ConnectOK_NC;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import ee.ut.jbizur.protocol.commands.net.SignalEnd_NC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BizurClient extends BizurNode {

    private static final Logger logger = LoggerFactory.getLogger(BizurClient.class);

    private final AtomicReference<Address> preferredAddress = new AtomicReference<>();
    private Iterator<Address> addressIterator;
    private final Object addressesLock = new Object();

    protected BizurClient(BizurSettings bizurSettings) throws IOException {
        super(bizurSettings);
        if (isAddressesAlreadyRegistered()) {
            createAddressIterator();
        }
    }

    @Override
    protected void handle(InternalCommand ic) {
        super.handle(ic);
        if (ic instanceof NodeAddressRegistered_IC) {
            createAddressIterator();
        }
        if (ic instanceof NodeAddressUnregistered_IC) {
            createAddressIterator();
        }
    }

    @Override
    public void handle(NetworkCommand command) {
        if (command instanceof ConnectOK_NC) {
            getSettings().registerAddress(command.getSenderAddress());
        }
        if (command instanceof SignalEnd_NC) {
            super.handle(command);
        }
    }

    @Override
    public BizurMap getMap(String mapName) {
        return bizurMaps.computeIfAbsent(mapName, s -> new BizurClientMap(s, this));
    }

    private void createAddressIterator() {
        synchronized (addressesLock) {
            List<Address> addresses = new ArrayList<>(getSettings().getMemberAddresses());
            addressIterator = new RoundRobin<>(addresses).iterator();
        }
    }

    void setPreferredAddress(Address address) {
        preferredAddress.set(address);
    }

    Address getMemberAddress() {
        Address addr = preferredAddress.get();
        if (addr != null) {
            return addr;
        }
        return nextAddress();
    }

    Address nextAddress() {
        return addressIterator.next();
    }
}
