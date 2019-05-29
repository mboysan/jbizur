package ee.ut.jbizur.network.io;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.role.Role;
import ee.ut.jbizur.role.bizur.BizurClientMock;
import ee.ut.jbizur.role.bizur.BizurNodeMock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManagerMock extends NetworkManager {

    public static final Map<Address, Role> ROLES = new ConcurrentHashMap<>();

    private final Address roleAddress;

    public NetworkManagerMock(BizurNodeMock nodeMock) {
        this((Role) nodeMock);
    }

    public NetworkManagerMock(BizurClientMock clientMock) {
        this((Role) clientMock);
    }

    private NetworkManagerMock(Role role) {
        super(role);
        roleAddress = role.getSettings().getAddress();
        ROLES.put(roleAddress, role);
    }

    @Override
    public NetworkManager start() {
        client = createClient();
        server = createServer();
        multicaster = null;
        return this;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        ROLES.remove(roleAddress);
    }

    @Override
    protected ClientMock createClient() {
        return new ClientMock(this);
    }

    @Override
    protected ServerMock createServer() {
        return new ServerMock(this);
    }

    @Override
    public ClientMock getClient() {
        return (ClientMock) super.getClient();
    }

    @Override
    public ServerMock getServer() {
        return (ServerMock) super.getServer();
    }
}
