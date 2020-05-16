package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.protocol.commands.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Set;

public class BizurClientMap extends BizurMap {

    private static final Logger logger = LoggerFactory.getLogger(BizurClientMap.class);

    private final BizurClient client;

    public BizurClientMap(String mapName, BizurClient node) {
        super(mapName, node);
        this.client = node;
    }

    private <T> T route(ClientRequest_NC cmd) throws BizurException {
        cmd.setMapName(mapName);
        return client.route(cmd);
    }

    @Override
    public Serializable get(Object key) {
        if (!(key instanceof Serializable)) {
            throw new IllegalArgumentException("key must be serializable");
        }
        checkReady();
        try {
            ClientResponse_NC response = route(
                    (ClientRequest_NC) new ClientApiGet_NC()
                            .setKey((Serializable) key)
                            .setReceiverAddress(client.getMemberAddress())
                            .setSenderAddress(client.getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            client.setPreferredAddress(response.getAssumedLeaderAddress());
            return (String) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Serializable put(Serializable key, Serializable value) {
        checkReady();
        try {
            ClientResponse_NC response = route(
                    (ClientRequest_NC) new ClientApiSet_NC()
                            .setKey(key)
                            .setVal(value)
                            .setReceiverAddress(client.getMemberAddress())
                            .setSenderAddress(client.getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            client.setPreferredAddress(response.getAssumedLeaderAddress());
            return (Serializable) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Serializable remove(Object key) {
        if (!(key instanceof Serializable)) {
            throw new IllegalArgumentException("key must be serializable");
        }
        checkReady();
        try {
            ClientResponse_NC response = route(
                    (ClientRequest_NC) new ClientApiDelete_NC()
                            .setKey((Serializable) key)
                            .setReceiverAddress(client.getMemberAddress())
                            .setSenderAddress(client.getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            client.setPreferredAddress(response.getAssumedLeaderAddress());
            return (Serializable) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Serializable> keySet() {
        checkReady();
        ClientResponse_NC response = null;
        try {
            response = route(
                    (ClientRequest_NC) new ClientApiIterKeys_NC()
                            .setReceiverAddress(client.nextAddress())
                            .setSenderAddress(client.getSettings().getAddress())
                            .setCorrelationId(IdUtil.generateId())
            );
            if (response.getAssumedLeaderAddress() != null) {
                client.setPreferredAddress(response.getAssumedLeaderAddress());
            }
            return (Set<Serializable>) response.getPayload();
        } catch (BizurException e) {
            // TODO: handle re-routing to another node
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void checkReady() {
        client.checkReady();
    }

    @Override
    public String toString() {
        return "BizurClientMap{} " + super.toString();
    }
}
