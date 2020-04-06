package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.common.protocol.commands.nc.NetworkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Predicate;

public class QuorumListener extends AbstractSyncedListener {

    private static final Logger logger = LoggerFactory.getLogger(QuorumListener.class);

    private final int totalSize;
    private final int quorumSize;
    private int ackCount = 0;
    private int nackCount = 0;
    private int testCount = 0;

    private final Predicate<NetworkCommand> commandPredicate;

    public QuorumListener(int totalSize, int quorumSize, Predicate<NetworkCommand> commandPredicate) {
        this(totalSize, quorumSize, 0, commandPredicate);
    }

    public QuorumListener(int totalSize, int quorumSize, long timeoutMillis, Predicate<NetworkCommand> commandPredicate) {
        super(totalSize, timeoutMillis);
        Objects.requireNonNull(commandPredicate);
        this.commandPredicate = commandPredicate;
        this.totalSize = totalSize;
        this.quorumSize = quorumSize;
    }

    @Override
    public synchronized boolean test(NetworkCommand command) {
        try {
            if (commandPredicate.test(command)) {
                ++ackCount;
                if (isMajorityAcked()) {
                    // majority acked
                    terminate();
                    return true;    // mark for remove
                }
            } else {
                ++nackCount;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        countdown();
        if (isMajorityNAcked() || (++testCount >= totalSize)) {
            terminate();
            return true;    // mark for remove
        }
        return false;
    }

    public boolean isMajorityNAcked() {
        return nackCount >= quorumSize;
    }

    public boolean isMajorityAcked() {
        return ackCount >= quorumSize;
    }
}
