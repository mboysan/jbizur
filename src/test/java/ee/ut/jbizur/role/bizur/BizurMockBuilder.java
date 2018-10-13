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
        MessageReceiverMock messageReceiverMock = new MessageReceiverMock();
        BizurNodeMock bizurNodeMock = new BizurNodeMock(
                getSettings(),
                new MulticasterMock(getSettings().getMulticastAddress(), null),
                new MessageSenderMock(),
                messageReceiverMock,
                new CountDownLatch(0)
        );
        messageReceiverMock.registerRole(bizurNodeMock);
        getSettings().registerRoleRef(bizurNodeMock);
        return bizurNodeMock;
    }

    @Override
    public BizurClientMock buildClient() throws InterruptedException {
        MessageReceiverMock messageReceiverMock = new MessageReceiverMock();
        BizurClientMock bizurClientMock = new BizurClientMock(
                getSettings(),
                new MulticasterMock(getSettings().getMulticastAddress(), null),
                new MessageSenderMock(),
                messageReceiverMock,
                new CountDownLatch(0)
            );
        messageReceiverMock.registerRole(bizurClientMock);
        getSettings().registerRoleRef(bizurClientMock);
        return bizurClientMock;
    }
}
