package ee.ut.jbizur.role.bizur;

public class BizurMockBuilder extends BizurBuilder {

    protected BizurMockBuilder() {
        super();
    }

    public static BizurMockBuilder mockBuilder() {
        return new BizurMockBuilder();
    }

    @Override
    public BizurNodeMock build() {
        BizurNodeMock bizurNodeMock = new BizurNodeMock(getSettings());
        getSettings().registerRoleRef(bizurNodeMock);
        return bizurNodeMock;
    }

    @Override
    public BizurClientMock buildClient() {
        BizurClientMock bizurClientMock = new BizurClientMock(getSettings());
        getSettings().registerRoleRef(bizurClientMock);
        return bizurClientMock;
    }
}
