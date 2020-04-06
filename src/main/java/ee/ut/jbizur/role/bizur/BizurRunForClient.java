package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.common.protocol.address.Address;
import ee.ut.jbizur.common.protocol.commands.nc.bizur.*;

import java.util.Set;

class BizurRunForClient extends BizurRun {
    BizurRunForClient(BizurNode node) {
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
        return createClientResponse(req, null, payload);
    }

    private ClientResponse_NC createClientResponse(ClientRequest_NC req, String bucketKey, Object payload) {
        return (ClientResponse_NC) new ClientResponse_NC()
                .setAssumedLeaderAddress(resolveLeader(bucketKey))
                .setRequest(req.toString())
                .setPayload(payload)
                .setSenderId(getSettings().getRoleId())
                .setSenderAddress(getSettings().getAddress())
                .ofRequest(req);
    }

    private Address resolveLeader(String key) {
        if (key == null) {
            return null;
        }
        return bucketContainer.getBucket(key).getLeaderAddress();
    }
}
