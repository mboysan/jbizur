package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.protocol.commands.MockNetworkCommand;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.RoleMock;
import ee.ut.jbizur.role.RoleSettings;
import org.junit.*;
import org.pmw.tinylog.Logger;
import utils.MultiThreadExecutor;

import java.util.Random;
import java.util.UUID;

@Ignore
public class MsgSendRecvMultiNodeTest {

    private final static int NODE_COUNT = 2;
    private RoleMock[] roleMocks = new RoleMock[NODE_COUNT];

    private Random random = getRandom();

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < roleMocks.length; i++) {
            roleMocks[i] = new RoleMock(new RoleSettings());
        }
    }

    @After
    public void tearDown() {
        for (int i = 0; i < roleMocks.length; i++) {
            roleMocks[i].receivedCommandsMap.clear();
            roleMocks[i].shutdown();
        }
    }

    private Random getRandom() {
        long seed = System.currentTimeMillis();
        return getRandom(seed);
    }

    private Random getRandom(long seed) {
        Logger.info("Seed: " + seed);
        return new Random(seed);
    }

    private RoleMock getRandomRole() {
        return getRole(-1);
    }
    private RoleMock getRole(int inx) {
        return roleMocks[inx == -1 ? random.nextInt(roleMocks.length) : inx];
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
            totalRecv += roleMock.receivedCommandsMap.size();
        }
        Assert.assertEquals(expCount, totalRecv);
        System.out.println("recv command count equals sent command count!");
    }

    private NetworkCommand generateCommand(RoleMock sender, RoleMock receiver) {
        return new MockNetworkCommand()
                .setMsgId(random.nextInt())
                .setPayload(UUID.randomUUID().toString())
                .setSenderId(sender.getSettings().getRoleId())
                .setSenderAddress(sender.getSettings().getAddress())
                .setReceiverAddress(receiver.getSettings().getAddress());
    }

}
