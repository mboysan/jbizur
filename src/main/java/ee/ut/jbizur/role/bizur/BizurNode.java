package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.config.LogConf;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.exceptions.RoleIsNotReadyError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.SyncMessageListener;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.*;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import ee.ut.jbizur.protocol.internal.InternalCommand;
import ee.ut.jbizur.protocol.internal.NodeDead_IC;
import ee.ut.jbizur.protocol.internal.SendFail_IC;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.role.RoleValidation;
import org.pmw.tinylog.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BizurNode extends Role {
    private boolean isReady;
    BucketContainer bucketContainer;

    BizurNode(BizurSettings settings) {
        super(settings);
        this.isReady = false;
        initBuckets();
    }

    protected void initBuckets() {
        this.bucketContainer = createBucketContainer();
    }

    protected BucketContainer createBucketContainer() {
        return new BucketContainer(Conf.get().consensus.bizur.bucketCount).initBuckets();
    }

    @Override
    public BizurSettings getSettings() {
        return (BizurSettings) super.getSettings();
    }

    protected void checkReady() throws RoleIsNotReadyError {
        try {
            RoleValidation.checkStateAndError(isReady, "Bizur node is not ready.");
        } catch (IllegalStateException e) {
            throw new RoleIsNotReadyError(e);
        }
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.<Void>supplyAsync(() -> {
            try {
                long multicastIntervalSec = Conf.get().network.multicast.intervalms;
                while (!checkNodesDiscovered()) {
                    Thread.sleep(multicastIntervalSec);
                }
                isReady = true;
                Logger.info(logMsg("Node initialized and ready!"));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    /* ***************************************************************************
     * Message Routing
     * ***************************************************************************/

    protected <T> T routeRequestAndGet(NetworkCommand command) {
        return routeRequestAndGet(command, command.getRetryCount());
    }

    protected <T> T routeRequestAndGet(NetworkCommand command, int retryCount) throws OperationFailedError {
        if (retryCount < 0) {
            throw new OperationFailedError(logMsg("Routing failed for command: " + command));
        }
        SyncMessageListener listener = SyncMessageListener.build()
                .withTotalProcessCount(1)
                .registerHandler(Nack_NC.class, (cmd,lst) -> {
                    lst.getPassedObjectRef().set(new SendFail_IC(cmd));
                    lst.end();
                })
                .registerHandler(ClientResponse_NC.class, (cmd, lst) -> {
                    lst.getPassedObjectRef().set(cmd);
                    lst.end();
                })
                .registerHandler(LeaderResponse_NC.class, (cmd, lst) -> {
                    lst.getPassedObjectRef().set(cmd.getPayload());
                    lst.end();
                });
        if (LogConf.isDebugEnabled()) {
            listener.withDebugInfo(logMsg("routeRequestAndGet : " + command));
        }
        attachMsgListener(listener);
        try {
            command.setMsgId(listener.getMsgId());
            if (LogConf.isDebugEnabled()) {
                Logger.debug(logMsg("routing request, retryLeft=[" + retryCount + "]: " + command));
            }
            sendMessage(command);

            if (listener.waitForResponses()) {
                T rsp = (T) listener.getPassedObjectRef().get();
                if(!(rsp instanceof SendFail_IC)) {
                    return rsp;
                } else {
                    Logger.warn(logMsg("Send failed: " + rsp.toString()));
                }
            }

            return routeRequestAndGet(command, retryCount - 1);

        } finally {
            detachMsgListener(listener);
        }
    }

    protected boolean initLeaderPerBucketElectionFlow() throws InterruptedException {
        return new BizurRun(this).initLeaderPerBucketElectionFlow();
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

    public String get(String key) throws RoleIsNotReadyError {
        checkReady();
        return new BizurRun(this).get(key);
    }
    private void getByLeader(ApiGet_NC getNc) {
        new BizurRun(this, getNc.getContextId()).getByLeader(getNc);
    }

    public boolean set(String key, String val) throws RoleIsNotReadyError {
        checkReady();
        return new BizurRun(this).set(key, val);
    }
    private void setByLeader(ApiSet_NC setNc) {
        new BizurRun(this, setNc.getContextId()).setByLeader(setNc);
    }

    public boolean delete(String key) throws RoleIsNotReadyError {
        checkReady();
        return new BizurRun(this).delete(key);
    }
    private void deleteByLeader(ApiDelete_NC deleteNc) {
        new BizurRun(this, deleteNc.getContextId()).deleteByLeader(deleteNc);
    }

    public Set<String> iterateKeys() throws RoleIsNotReadyError {
        checkReady();
        return new BizurRun(this).iterateKeys();
    }
    private void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        new BizurRun(this, iterKeysNc.getContextId()).iterateKeysByLeader(iterKeysNc);
    }

    private void handleLeaderElection(LeaderElectionRequest_NC ler) {
        new BizurRun(this, ler.getContextId()).handleLeaderElection(ler);
    }

    /* ***************************************************************************
     * Message Handling
     * ***************************************************************************/

    protected boolean validateCommand(NetworkCommand command) {
        if (command.getSenderAddress() != null && command.getReceiverAddress() != null) {
            if (command.getSenderAddress().equals(command.getReceiverAddress())) {
                return syncMessageListeners.get(command.getMsgId()) != null;
            }
        }
        return true;
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        super.handleNetworkCommand(command);

        if (!validateCommand(command)) {
            if (LogConf.isDebugEnabled()) {
                Logger.debug(logMsg("command discarded [" + command + "]"));
            }
            return;
        }

        if (command instanceof LeaderElectionRequest_NC) {
            handleLeaderElection((LeaderElectionRequest_NC) command);
        }

        if (command instanceof ReplicaWrite_NC){
            replicaWrite((ReplicaWrite_NC) command);
        }
        if (command instanceof ReplicaRead_NC){
            replicaRead(((ReplicaRead_NC) command));
        }
        if (command instanceof PleaseVote_NC) {
            pleaseVote(((PleaseVote_NC) command));
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
                sendMessage(response);
            }
        }
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {
        if(command instanceof SendFail_IC) {
            new BizurRun(this).handleSendFailureWithoutRetry((SendFail_IC) command);
        }
        if(command instanceof NodeDead_IC) {
            handleNodeFailure(((NodeDead_IC) command).getNodeAddress());
        }
    }

    @Override
    protected void attachMsgListener(SyncMessageListener listener) {
        super.attachMsgListener(listener);
    }
    @Override
    protected void detachMsgListener(SyncMessageListener listener) {
        super.detachMsgListener(listener);
    }
    @Override
    public String logMsg(String msg) {
        return super.logMsg(msg);
    }
    @Override
    protected void sendMessage(NetworkCommand message) {
        super.sendMessage(message);
    }
    @Override
    protected boolean pingAddress(Address address) {
        return super.pingAddress(address);
    }
}
