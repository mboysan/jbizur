package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.messenger.MessageReceiverMock;
import ee.ut.jbizur.network.messenger.MessageSenderMock;
import ee.ut.jbizur.network.messenger.MulticasterMock;

import java.util.concurrent.CountDownLatch;

public class BizurMockBuilder extends BizurBuilder {

    protected BizurMockBuilder() {
        super();
    }

    public static BizurMockBuilder mockBuilder() {
        return new BizurMockBuilder();
    }

    @Override
    public BizurNodeMock build() throws InterruptedException {
        BizurNodeMock bizurNodeMock = new BizurNodeMock(
                getConfig(),
                new MulticasterMock(getConfig().getMulticastAddress(), null),
                new MessageSenderMock(),
                new MessageReceiverMock(),
                new CountDownLatch(0)
        );
        getConfig().registerRoleRef(bizurNodeMock);
        return bizurNodeMock;
    }

    @Override
    public BizurClientMock buildClient() throws InterruptedException {
        BizurClientMock bizurClientMock = new BizurClientMock(
                getConfig(),
                new MulticasterMock(getConfig().getMulticastAddress(), null),
                new MessageSenderMock(),
                new MessageReceiverMock(),
                new CountDownLatch(0)
            );
        getConfig().registerRoleRef(bizurClientMock);
        return bizurClientMock;
    }
}
