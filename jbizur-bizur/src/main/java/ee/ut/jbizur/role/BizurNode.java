package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.InternalCommand;
import ee.ut.jbizur.protocol.commands.intl.NodeDead_IC;
import ee.ut.jbizur.protocol.commands.intl.SendFail_IC;
import ee.ut.jbizur.protocol.commands.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BizurNode extends Role {

    private static final Logger logger = LoggerFactory.getLogger(BizurNode.class);

    private boolean isReady;
    BucketContainer bucketContainer;

    BizurNode(BizurSettings settings) throws IOException {
        super(settings);
        this.isReady = false;
        initBuckets();
    }

    protected void initBuckets() {
        this.bucketContainer = createBucketContainer();
    }

    protected BucketContainer createBucketContainer() {
        return new BucketContainer(CoreConf.get().consensus.bizur.bucketCount);
    }

    @Override
    public BizurSettings getSettings() {
        return (BizurSettings) super.getSettings();
    }

    protected void checkReady() {
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

    <T> T route(NetworkCommand command) throws RoutingFailedException {
        for (int i = 0; i < command.getRetryCount() + 1; i++) {
            try {
                NetworkCommand cmd = sendRecv(command);
                if (cmd instanceof LeaderResponse_NC) {
                    Object payload = cmd.getPayload();
                    if (payload instanceof IllegalLeaderOperationException) {
                        throw new RoutingFailedException((Throwable) payload);
                    }
                    return (T) payload;
                }
                return (T) cmd;
            } catch (Exception e) {
                logger.warn("rout error count={}", i);
            }
        }
        throw new RoutingFailedException(logMsg("Routing failed for command: " + command));
    }

    private void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        new BizurRun(this, pleaseVoteNc.getContextId()).pleaseVote(pleaseVoteNc);
    }

    private void replicaWrite(ReplicaWrite_NC replicaWriteNc){
        new BizurRun(this).replicaWrite(replicaWriteNc);
    }

    private void replicaRead(ReplicaRead_NC replicaReadNc){
        new BizurRun(this).replicaRead(replicaReadNc);
    }

    public String get(String key) {
        checkReady();
        return new BizurRun(this).get(key);
    }
    private void getByLeader(ApiGet_NC getNc) {
        new BizurRun(this, getNc.getContextId()).getByLeader(getNc);
    }

    public boolean set(String key, String val) {
        checkReady();
        return new BizurRun(this).set(key,val);
    }
    private void setByLeader(ApiSet_NC setNc) {
        new BizurRun(this, setNc.getContextId()).setByLeader(setNc);
    }

    public boolean delete(String key) {
        checkReady();
        return new BizurRun(this).delete(key);
    }
    private void deleteByLeader(ApiDelete_NC deleteNc) {
        new BizurRun(this, deleteNc.getContextId()).deleteByLeader(deleteNc);
    }

    public Set<String> iterateKeys() {
        checkReady();
        return new BizurRun(this).iterateKeys();
    }
    private void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        new BizurRun(this, iterKeysNc.getContextId()).iterateKeysByLeader(iterKeysNc);
    }

    private void whoIsLeader(WhoIsLeaderRequest_NC wilNc) {
        new BizurRun(this).whoIsLeader(wilNc);
    }

    //TODO: to be removed
    Address resolveLeader(int bucketIndex) {
        return new BizurRun(this).resolveLeader(bucketIndex);
    }
    //TODO: to be removed
    void startElection(int bucketIndex) {
        new BizurRun(this).startElection(bucketIndex);
    }

    /* ***************************************************************************
     * Message Handling
     * ***************************************************************************/

    @Override
    public void handle(NetworkCommand command) {
        super.handle(command);

        if (command instanceof ReplicaWrite_NC){
            replicaWrite((ReplicaWrite_NC) command);
        }
        if (command instanceof ReplicaRead_NC){
            replicaRead(((ReplicaRead_NC) command));
        }
        if (command instanceof PleaseVote_NC) {
            pleaseVote(((PleaseVote_NC) command));
        }
        if (command instanceof WhoIsLeaderRequest_NC) {
            whoIsLeader((WhoIsLeaderRequest_NC) command);
        }

        /* Internal API routed requests */
        if(command instanceof ApiGet_NC){
            getByLeader((ApiGet_NC) command);
        }
        if(command instanceof ApiSet_NC){
            setByLeader((ApiSet_NC) command);
        }
        if(command instanceof ApiDelete_NC){
            deleteByLeader((ApiDelete_NC) command);
        }
        if(command instanceof ApiIterKeys_NC){
            iterateKeysByLeader((ApiIterKeys_NC) command);
        }

        /* Client Request-response */
        if (command instanceof ClientRequest_NC) {
            BizurRunForClient bcRun = new BizurRunForClient(this);
            ClientResponse_NC response = null;
            if(command instanceof ClientApiGet_NC){
                response = bcRun.get((ClientApiGet_NC) command);
            }
            if(command instanceof ClientApiSet_NC){
                response = bcRun.set((ClientApiSet_NC) command);
            }
            if(command instanceof ClientApiDelete_NC){
                response = bcRun.delete((ClientApiDelete_NC) command);
            }
            if(command instanceof ClientApiIterKeys_NC){
                response = bcRun.iterateKeys((ClientApiIterKeys_NC) command);
            }
            if (response != null) {
                try {
                    send(response);
                } catch (IOException e) {
                    logger.error(logMsg(e.getMessage()), e);
                }
            }
        }
    }

    @Override
    protected void handle(InternalCommand ic) {
        if(ic instanceof SendFail_IC) {
            new BizurRun(this).handleSendFailureWithoutRetry((SendFail_IC) ic);
        }
        if(ic instanceof NodeDead_IC) {
            handleNodeFailure(((NodeDead_IC) ic).getNodeAddress());
        }
    }

    @Override
    public String logMsg(String msg) {
        return super.logMsg(msg);
    }

    @Override
    protected boolean ping(Address address) throws IOException {
        return super.ping(address);
    }
}