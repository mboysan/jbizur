package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.messenger.NetworkManagerMock;
import ee.ut.jbizur.network.messenger.ServerMock;
import ee.ut.jbizur.network.messenger.ClientMock;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;

public class BizurClientMock extends BizurClient {

    public BizurClientMock(BizurSettings bizurSettings) {
        super(bizurSettings);
    }

    @Override
    protected void initRole() {
        this.networkManager = new NetworkManagerMock(this).start();
    }

    public void registerRoles(Role[] roles){
        for (Role role : roles) {
            registerRole(role);
        }
    }

    public void registerRole(Role role) {
        ((ClientMock) networkManager.getClient()).registerRole(role);
    }

    public void sendCommandToMessageReceiver(NetworkCommand command) {
        ((ServerMock) networkManager.getServer()).handleNetworkCommand(command);
    }
}
