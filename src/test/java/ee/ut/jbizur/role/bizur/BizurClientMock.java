package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.io.NetworkManagerMock;

public class BizurClientMock extends BizurClient {

    public BizurClientMock(BizurSettings bizurSettings) {
        super(bizurSettings);
    }

    @Override
    protected void initRole() {
        this.networkManager = new NetworkManagerMock(this).start();
    }

    public NetworkManagerMock getNetworkManager() {
        return (NetworkManagerMock) networkManager;
    }
}
