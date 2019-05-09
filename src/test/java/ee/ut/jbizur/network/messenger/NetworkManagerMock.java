package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.role.Role;

public class NetworkManagerMock extends NetworkManager {

    public NetworkManagerMock(Role role) {
        super(role);
    }

    @Override
    protected AbstractClient createClient() {
        return new ClientMock(role);
    }

    @Override
    protected AbstractServer createServer() {
        return new ServerMock(role);
    }
}
