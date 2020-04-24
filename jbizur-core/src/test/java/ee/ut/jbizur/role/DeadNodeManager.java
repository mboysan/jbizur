package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.address.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DeadNodeManager {
    private static final Logger logger = LoggerFactory.getLogger(DeadNodeManager.class);

    private static final Set<Address> DEAD_NODE_ADDRESSES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DeadNodeManager() {}

    public static boolean kill(Role node) {
        logger.info("killing node={}", node);
        return DEAD_NODE_ADDRESSES.add(node.getSettings().getAddress());
    }

    public static boolean revive(Role node) {
        logger.info("reviving node={}", node);
        return DEAD_NODE_ADDRESSES.remove(node.getSettings().getAddress());
    }

    public static boolean isDead(Role node) {
        return isDead(node.getSettings().getAddress());
    }

    public static boolean isDead(Address address) {
        return DEAD_NODE_ADDRESSES.contains(address);
    }
}