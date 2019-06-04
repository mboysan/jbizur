package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.datastore.bizur.BucketContainer;
import ee.ut.jbizur.exceptions.OperationFailedError;
import ee.ut.jbizur.exceptions.RoleIsNotReadyError;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.handlers.CallbackState;
import ee.ut.jbizur.network.handlers.QuorumState;
import ee.ut.jbizur.protocol.commands.ICommand;
import ee.ut.jbizur.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.protocol.commands.ic.NodeDead_IC;
import ee.ut.jbizur.protocol.commands.ic.SendFail_IC;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.bizur.*;
import ee.ut.jbizur.protocol.commands.nc.common.Nack_NC;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.role.RoleValidation;
import org.pmw.tinylog.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
        return new BucketContainer(Conf.get().consensus.bizur.bucketCount);
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

    public <T> T routeRequestAndGet(NetworkCommand command) throws OperationFailedError {
        AtomicReference<Object> respRef = new AtomicReference<>();
        Predicate<ICommand> cdHandler = (cmd) -> {
            if (cmd instanceof Nack_NC) {
                respRef.set(new SendFail_IC((Nack_NC) cmd));
                return true;
            } else if (cmd instanceof ClientResponse_NC) {
                respRef.set(cmd);
                return true;
            } else if (cmd instanceof LeaderResponse_NC) {
                respRef.set(((LeaderResponse_NC) cmd).getPayload());
                return true;
            }
            return false;
        };
        for (int i = 0; i < command.getRetryCount() + 1; i++) {
            if(sendMessage(command, null, cdHandler).awaitResponses()) {
                T retVal = (T) respRef.get();
                if (!(retVal instanceof SendFail_IC)) {
                    return retVal;
                }
            }
        }
        throw new OperationFailedError(logMsg("Routing failed for command: " + command));
    }

    protected boolean initLeaderPerBucketElectionFlow() throws InterruptedException {
        try (BizurRun br = new BizurRun(this)) {
            return br.initLeaderPerBucketElectionFlow();
        }
    }

    private void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        new BizurRun(this, pleaseVoteNc.getContextId()).pleaseVote(pleaseVoteNc);
    }

    private void replicaWrite(ReplicaWrite_NC replicaWriteNc){
        try (BizurRun br = new BizurRun(this)) {
            br.replicaWrite(replicaWriteNc);
        }
    }

    private void replicaRead(ReplicaRead_NC replicaReadNc){
        try (BizurRun br = new BizurRun(this)) {
            br.replicaRead(replicaReadNc);
        }
    }

    public String get(String key) throws RoleIsNotReadyError {
        checkReady();
        try (BizurRun br = new BizurRun(this)) {
            return br.get(key);
        }
    }
    private void getByLeader(ApiGet_NC getNc) {
        new BizurRun(this, getNc.getContextId()).getByLeader(getNc);
    }

    public boolean set(String key, String val) throws RoleIsNotReadyError {
        checkReady();
        try (BizurRun br = new BizurRun(this)) {
            return br.set(key,val);
        }
    }
    private void setByLeader(ApiSet_NC setNc) {
        new BizurRun(this, setNc.getContextId()).setByLeader(setNc);
    }

    public boolean delete(String key) throws RoleIsNotReadyError {
        checkReady();
        try (BizurRun br = new BizurRun(this)) {
            return br.delete(key);
        }
    }
    private void deleteByLeader(ApiDelete_NC deleteNc) {
        new BizurRun(this, deleteNc.getContextId()).deleteByLeader(deleteNc);
    }

    public Set<String> iterateKeys() throws RoleIsNotReadyError {
        checkReady();
        try (BizurRun br = new BizurRun(this)) {
            return br.iterateKeys();
        }
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

    @Override
    public void handleNetworkCommand(NetworkCommand command) {
        super.handleNetworkCommand(command);

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
            try (BizurRunForClient bcRun = new BizurRunForClient(this)) {
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
    }

    @Override
    public void handleInternalCommand(InternalCommand command) {
        if(command instanceof SendFail_IC) {
            try (BizurRun br = new BizurRun(this)) {
                br.handleSendFailureWithoutRetry((SendFail_IC) command);
            }
        }
        if(command instanceof NodeDead_IC) {
            handleNodeFailure(((NodeDead_IC) command).getNodeAddress());
        }
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
    protected CallbackState sendMessage(NetworkCommand command, Predicate<ICommand> handler, Predicate<ICommand> countdownHandler) {
        return super.sendMessage(command, handler, countdownHandler);
    }

    @Override
    protected QuorumState sendMessageToQuorum(Supplier<NetworkCommand> cmdSupplier, Integer contextId, Integer msgId, Predicate<ICommand> handler, Predicate<ICommand> countdownHandler) {
        return super.sendMessageToQuorum(cmdSupplier, contextId, msgId, handler, countdownHandler);
    }
    @Override
    protected boolean pingAddress(Address address) {
        return super.pingAddress(address);
    }

    void handleCmd(ICommand cmd) {
        networkManager.handleCmd(cmd);
    }
}
