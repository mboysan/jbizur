package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.InternalCommand;
import ee.ut.jbizur.protocol.commands.net.LeaderResponse_NC;
import ee.ut.jbizur.protocol.commands.net.MapRequest_NC;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public class BizurNode extends Role {

    private static final Logger logger = LoggerFactory.getLogger(BizurNode.class);

    final Map<String, BizurMap> bizurMaps = new ConcurrentHashMap<>();

    private boolean isReady;

    BizurNode(BizurSettings settings) throws IOException {
        super(settings);
        this.isReady = false;
    }

    @Override
    public BizurSettings getSettings() {
        return (BizurSettings) super.getSettings();
    }

    void checkReady() {
        RoleValidation.checkStateAndError(isReady, "Bizur node is not ready.");
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.<Void>supplyAsync(() -> {
            try {
                long multicastIntervalSec = CoreConf.get().network.multicast.intervalms;
                while (!checkNodesDiscovered()) {
                    Thread.sleep(multicastIntervalSec);
                }
                isReady = true;
                logger.info(logMsg("Node initialized and ready!"));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    /* ***************************************************************************
     * Message Routing
     * ***************************************************************************/

    <T> T route(NetworkCommand command) throws BizurException {
        Exception finalEx = null;
        for (int i = 0; i < command.getRetryCount() + 1; i++) {
            try {
                NetworkCommand cmd = sendRecv(command);
                if (cmd instanceof LeaderResponse_NC) {
                    Object payload = cmd.getPayload();
                    if (payload instanceof Exception) {
                        logger.warn("payload has exception={}", payload);
                        finalEx = (Exception) payload;
                        continue;
                    }
                    return (T) payload;
                }
                return (T) cmd;
            } catch (Exception e) {
                logger.warn("route error count={}", i, e);
            }
        }
        if (finalEx instanceof BizurException) {
            throw (BizurException) finalEx;
        }
        // still some other issue?
        throw new BizurException(logMsg("Routing failed for command: " + command), finalEx);
    }

    /* ***************************************************************************
     * Message Handling
     * ***************************************************************************/

    @Override
    public void handle(NetworkCommand command) {
        super.handle(command);

        if (command instanceof MapRequest_NC) {
            MapRequest_NC mapCmd = (MapRequest_NC) command;
            String mapName = mapCmd.getMapName();
            BizurMap bizurMap = getMap(mapName);
            bizurMap.handle(mapCmd);
        }
    }

    @Override
    protected void handle(InternalCommand ic) {
    }

    @Override
    public String logMsg(String msg) {
        return super.logMsg(msg);
    }

    @Override
    protected boolean ping(Address address) throws IOException {
        return super.ping(address);
    }

    /* ***************************************************************************
     * Public methods
     * ***************************************************************************/

    public BizurMap getMap(String mapName) {
        return bizurMaps.computeIfAbsent(mapName, s -> new BizurMap(s, this));
    }
}
