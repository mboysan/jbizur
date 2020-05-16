package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.commands.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class BizurMap implements Map<Serializable, Serializable> {

    private static final Logger logger = LoggerFactory.getLogger(BizurMap.class);

    final BizurNode node;
    final String mapName;
    final BucketContainer bucketContainer;

    public BizurMap(String mapName, BizurNode node) {
        this.mapName = mapName;
        this.node = node;
        this.bucketContainer = new BucketContainer(node.getSettings().getRoleId(), node.getSettings().getNumBuckets());
    }

    /* -------------------------------------------
     * Unimplemented methods
     * ------------------------------------------- */

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Serializable> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<Serializable, Serializable>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends Serializable, ? extends Serializable> m) {
        throw new UnsupportedOperationException();
    }

    /* -------------------------------------------
     * Straightforward impl.
     * ------------------------------------------- */

    @Override
    public boolean isEmpty() {
        return keySet().isEmpty();
    }

    /* -------------------------------------------
     * Map and Bizur specific
     * ------------------------------------------- */

    @Override
    public Serializable get(Object key) {
        checkReady();
        return new BizurRun(this).get((Serializable) key);
    }
    private void getByLeader(ApiGet_NC getNc) {
        new BizurRun(this, getNc.getContextId()).getByLeader(getNc);
    }

    @Override
    public Serializable put(Serializable key, Serializable value) {
        checkReady();
        return new BizurRun(this).set(key, value);
    }
    private void setByLeader(ApiSet_NC setNc) {
        new BizurRun(this, setNc.getContextId()).setByLeader(setNc);
    }

    @Override
    public Serializable remove(Object key) {
        checkReady();
        return new BizurRun(this).delete((Serializable) key);
    }
    private void deleteByLeader(ApiDelete_NC deleteNc) {
        new BizurRun(this, deleteNc.getContextId()).deleteByLeader(deleteNc);
    }

    @Override
    public Set<Serializable> keySet() {
        checkReady();
        return new BizurRun(this).iterateKeys();
    }
    private void iterateKeysByLeader(ApiIterKeys_NC iterKeysNc) {
        new BizurRun(this, iterKeysNc.getContextId()).iterateKeysByLeader(iterKeysNc);
    }

    private void checkReady() {
        node.checkReady();
    }

    private void pleaseVote(PleaseVote_NC pleaseVoteNc) {
        new BizurRun(this, pleaseVoteNc.getContextId()).pleaseVote(pleaseVoteNc);
    }

    private void replicaWrite(ReplicaWrite_NC replicaWriteNc){
        new BizurRun(this, replicaWriteNc.getContextId()).replicaWrite(replicaWriteNc);
    }

    private void replicaRead(ReplicaRead_NC replicaReadNc){
        new BizurRun(this, replicaReadNc.getContextId()).replicaRead(replicaReadNc);
    }

    //ForTestingOnly
    void startElection(int index) {
        new BizurRun(this).startElection(index);
    }

    void handle(MapRequest_NC command) {
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
            BizurClientRun bcRun = new BizurClientRun(this);
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
                    node.send(response);
                } catch (IOException e) {
                    logger.error(node.logMsg(e.getMessage()), e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BizurMap{" +
                "mapName='" + mapName + '\'' +
                '}';
    }
}
