package ee.ut.jbizur.role;

import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.net.*;

import java.io.Serializable;
import java.util.Set;

class BizurClientRun extends BizurRun {
    BizurClientRun(BizurNode node) {
        super(node);
    }

    ClientResponse_NC set(ClientApiSet_NC req) {
        Boolean payload = set(req.getKey(), req.getVal());
        return createClientResponse(req, req.getKey(), payload);
    }

    ClientResponse_NC get(ClientApiGet_NC req) {
        String payload = get(req.getKey());
        return createClientResponse(req, req.getKey(), payload);
    }

    ClientResponse_NC delete(ClientApiDelete_NC req) {
        Boolean payload = delete(req.getKey());
        return createClientResponse(req, req.getKey(), payload);
    }

    ClientResponse_NC iterateKeys(ClientApiIterKeys_NC req) {
        Set<String> payload = iterateKeys();
        return createClientResponse(req, null, (Serializable) payload);
    }

    private ClientResponse_NC createClientResponse(ClientRequest_NC req, String bucketKey, Serializable payload) {
        return (ClientResponse_NC) new ClientResponse_NC()
                .setAssumedLeaderAddress(resolveLeader(bucketKey))
                .setRequest(req.toString())
                .setPayload(payload)
                .setSenderAddress(getSettings().getAddress())
                .ofRequest(req);
    }

    Address resolveLeader(String key) {
        if (key == null) {
            return null;
        }
        int index = bucketContainer.hashKey(key);
        Bucket bucket = bucketContainer.tryAndLockBucket(index);
        if (bucket != null) {
            try {
                return bucket.getLeaderAddress();
            } finally {
                bucket.unlock();
            }
        }
        return null;
    }
}
