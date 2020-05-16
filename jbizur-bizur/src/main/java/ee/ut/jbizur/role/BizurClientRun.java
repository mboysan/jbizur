package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.net.*;

import java.io.Serializable;
import java.util.Set;

class BizurClientRun extends BizurRun {
    private final BizurMap bizurMap;

    BizurClientRun(BizurMap bizurMap) {
        super(bizurMap);
        this.bizurMap = bizurMap;
    }

    ClientResponse_NC set(ClientApiSet_NC req) {
        Serializable payload = set(req.getKey(), req.getVal());
        return createClientResponse(req, req.getKey(), payload);
    }

    ClientResponse_NC get(ClientApiGet_NC req) {
        Serializable payload = get(req.getKey());
        return createClientResponse(req, req.getKey(), payload);
    }

    ClientResponse_NC delete(ClientApiDelete_NC req) {
        Serializable payload = delete(req.getKey());
        return createClientResponse(req, req.getKey(), payload);
    }

    ClientResponse_NC iterateKeys(ClientApiIterKeys_NC req) {
        Set<Serializable> payload = iterateKeys();
        return createClientResponse(req, null, (Serializable) payload);
    }

    private ClientResponse_NC createClientResponse(ClientRequest_NC req, Serializable bucketKey, Serializable payload) {
        return (ClientResponse_NC) new ClientResponse_NC()
                .setAssumedLeaderAddress(resolveLeader(bucketKey))
                .setRequest(req.toString())
                .setPayload(payload)
                .setSenderAddress(getSettings().getAddress())
                .ofRequest(req);
    }

    private Address resolveLeader(Serializable key) {
        if (key == null) {
            return null;
        }
        return resolveLeader(bizurMap.bucketContainer.hashKey(key));
    }
}
