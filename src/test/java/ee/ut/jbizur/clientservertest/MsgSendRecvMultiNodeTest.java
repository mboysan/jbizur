package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.protocol.commands.MockNetworkCommand;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.RoleMock;
import ee.ut.jbizur.role.RoleSettings;
import org.junit.*;
import utils.MultiThreadExecutor;

import java.util.UUID;

import static utils.TestUtils.getRandom;

@Ignore
public class MsgSendRecvMultiNodeTest {

    private final static int NODE_COUNT = 2;
    private RoleMock[] roleMocks = new RoleMock[NODE_COUNT];

    @Before
    public void setUp() {
        for (int i = 0; i < roleMocks.length; i++) {
            roleMocks[i] = new RoleMock(new RoleSettings());
        }
    }

    @After
    public void tearDown() {
        for (int i = 0; i < roleMocks.length; i++) {
            roleMocks[i].shutdown();
        }
    }

    private RoleMock getRandomRole() {
        return getRole(-1);
    }
    private RoleMock getRole(int inx) {
        return roleMocks[inx == -1 ? getRandom().nextInt(roleMocks.length) : inx];
    }

    @Test
    public void testSimpleMessageSendRecvWith2Nodes() throws Throwable {
        int testCount = 1000;
        RoleMock sender = getRole(0);
        RoleMock receiver = getRole(1);
        for (int i = 0; i < testCount; i++) {
            sender.getMessageProcessor().getClient().send(generateCommand(sender, receiver));
        }
        checkReceivedCommandsSize(testCount);
    }

    @Test
    public void testSimpleMessageSendRecvMixed() throws Throwable {
        int testCount = 1000;
        for (int i = 0; i < testCount; i++) {
            RoleMock sender = getRandomRole();
            RoleMock receiver = getRandomRole();
            sender.getMessageProcessor().getClient().send(generateCommand(sender, receiver));
        }
        checkReceivedCommandsSize(testCount);
    }

    @Test
    public void testMultithreadMessageSendRecv() throws Throwable {
        int testCount = 1000;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                RoleMock sender = getRandomRole();
                RoleMock receiver = getRandomRole();
                sender.getMessageProcessor().getClient().send(generateCommand(sender, receiver));
            });
        }
        executor.endExecution();

        checkReceivedCommandsSize(testCount);
    }

    private void checkReceivedCommandsSize(int expCount) throws InterruptedException {
        Thread.sleep(5000);
        int totalRecv = 0;
        for (RoleMock roleMock : roleMocks) {
            totalRecv += roleMock.recvCmdCount.get();
        }
        Assert.assertEquals(expCount, totalRecv);
        System.out.println("recv command count equals sent command count!");
    }

    private NetworkCommand generateCommand(RoleMock sender, RoleMock receiver) {
        return new MockNetworkCommand()
                .setMsgId(getRandom().nextInt())
                .setPayload(UUID.randomUUID().toString())
                .setSenderId(sender.getSettings().getRoleId())
                .setSenderAddress(sender.getSettings().getAddress())
                .setReceiverAddress(receiver.getSettings().getAddress());
    }

}
