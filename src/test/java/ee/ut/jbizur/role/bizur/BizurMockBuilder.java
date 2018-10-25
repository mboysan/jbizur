package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.messenger.MessageProcessorMock;
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
        MessageProcessorMock messageProcessor = new MessageProcessorMock();
        BizurNodeMock bizurNodeMock = new BizurNodeMock(getSettings(), messageProcessor);
        messageProcessor.registerRole(bizurNodeMock);
        getSettings().registerRoleRef(bizurNodeMock);
        bizurNodeMock.initRole();
        return bizurNodeMock;
    }

    @Override
    public BizurClientMock buildClient() throws InterruptedException {
        MessageProcessorMock messageProcessor = new MessageProcessorMock();
        BizurClientMock bizurClientMock = new BizurClientMock(getSettings(), messageProcessor);
        messageProcessor.registerRole(bizurClientMock);
        getSettings().registerRoleRef(bizurClientMock);
        bizurClientMock.initRole();
        return bizurClientMock;
    }
}
