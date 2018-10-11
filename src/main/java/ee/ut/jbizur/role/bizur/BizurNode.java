package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.BizurConfig;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.exceptions.RoleIsNotReadyError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.IMessageReceiver;
import ee.ut.jbizur.network.messenger.IMessageSender;
import ee.ut.jbizur.network.messenger.Multicaster;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

public class BizurNode extends Role {
    private boolean isReady;
    BucketContainer bucketContainer;

    BizurNode(BizurSettings settings) throws InterruptedException {
        this(settings, null, null, null, null);
    }

    @ForTestingOnly
    protected BizurNode(BizurSettings settings,
                        Multicaster multicaster,
                        IMessageSender messageSender,
                        IMessageReceiver messageReceiver,
                        CountDownLatch readyLatch) throws InterruptedException {
        super(settings, multicaster, messageSender, messageReceiver, readyLatch);

        this.isReady = false;
        initBuckets();
    }

    protected void initBuckets() {
        this.bucketContainer = createBucketContainer();
    }

    protected BucketContainer createBucketContainer() {
        return new BucketContainer(BizurConfig.getBucketCount()).initBuckets();
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
                long multicastIntervalSec = NodeConfig.getMulticastIntervalMs();
                while (!checkNodesDiscovered()) {
                    Thread.sleep(multicastIntervalSec);
                }
                isReady = initLeaderPerBucketElectionFlow();
                if (!isReady) {
                    throw new IllegalStateException(logMsg("bucket leader election flow failed!"));
                }
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
                .withDebugInfo(logMsg("routeRequestAndGet : " + command))
                .registerHandler(Nack_NC.class, (cmd,lst) -> {
                    lst.getPassedObjectRef().set(new SendFail_IC(cmd));
                    lst.end();
                })
                .registerHandler(LeaderResponse_NC.class, (cmd, lst) -> {
                    lst.getPassedObjectRef().set(cmd.getPayload());
                    lst.end();
                });
        attachMsgListener(listener);
        try {
            command.setMsgId(listener.getMsgId());
            Logger.debug(logMsg("routing request, retryLeft=[" + retryCount + "]: " + command));
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
            if (command.getSenderAddress().isSame(command.getReceiverAddress())) {
                return syncMessageListeners.get(command.getMsgId()) != null;
            }
        }
        return true;
    }

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        super.handleNetworkCommand(command);

        if (!validateCommand(command)) {
            Logger.debug(logMsg("command discarded [" + command + "]"));
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

        String uniqueKey = UUID.randomUUID().toString();
        Object payload = uniqueKey;
        /* Internal API routed requests */
        if(command instanceof ClientApiGet_NC){
            payload = get(((ClientApiGet_NC) command).getKey());
        }
        if(command instanceof ClientApiSet_NC){
            payload = set(((ClientApiSet_NC) command).getKey(), ((ClientApiSet_NC) command).getVal());
        }
        if(command instanceof ClientApiDelete_NC){
            payload = delete(((ClientApiDelete_NC) command).getKey());
        }
        if(command instanceof ClientApiIterKeys_NC){
            payload = iterateKeys();
        }

        if(!uniqueKey.equals(payload)) {
            sendMessage(new LeaderResponse_NC()
                    .setPayload(payload)
                    .setSenderId(getSettings().getRoleId())
                    .setReceiverAddress(command.getSenderAddress())
                    .setSenderAddress(getSettings().getAddress())
                    .setMsgId(command.getMsgId()));
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
    protected String logMsg(String msg) {
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
