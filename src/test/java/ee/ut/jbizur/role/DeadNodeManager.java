package ee.ut.jbizur.role;

import ee.ut.jbizur.common.protocol.address.Address;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DeadNodeManager {

    private static final Set<Address> DEAD_NODE_ADDRESSES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DeadNodeManager() {}

    public static boolean kill(Role node) {
        return DEAD_NODE_ADDRESSES.add(node.getSettings().getAddress());
    }

    public static boolean revive(Role node) {
        return DEAD_NODE_ADDRESSES.remove(node.getSettings().getAddress());
    }

    public static boolean isDead(Role node) {
        return isDead(node.getSettings().getAddress());
    }

    public static boolean isDead(Address address) {
        return DEAD_NODE_ADDRESSES.contains(address);
    }
}