package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.protocol.commands.MockNetworkCommand;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.RoleMock;
import ee.ut.jbizur.role.RoleSettings;
import org.junit.*;
import utils.MultiThreadExecutor;

import java.util.UUID;

import static utils.TestUtils.getRandom;

@Ignore
public class MsgSendRecvTest {

    static {
        Conf.setConfigFromResources("jbizur_integ_test.conf");
    }

    private RoleMock roleMock;

    @Before
    public void setUp() throws Exception {
        roleMock = new RoleMock(new RoleSettings());
    }

    @After
    public void tearDown() {
        roleMock.shutdown();
    }

    @Test
    public void testSimpleMessageSendRecv() throws Throwable {
        int testCount = 1000;
        for (int i = 0; i < testCount; i++) {
            roleMock.getNetworkManager().getClient().send(generateCommand());
        }

        Thread.sleep(5000);

        Assert.assertEquals(testCount, roleMock.recvCmdCount.get());
    }

    @Test
    public void testMultithreadMessageSendRecv() throws Throwable {
        int testCount = 1000;
        MultiThreadExecutor executor = new MultiThreadExecutor();
        for (int i = 0; i < testCount; i++) {
            executor.execute(() -> {
                roleMock.getNetworkManager().getClient().send(generateCommand());
            });
        }

        executor.endExecution();
        Thread.sleep(5000);

        Assert.assertEquals(testCount, roleMock.recvCmdCount.get());
    }

    protected NetworkCommand generateCommand() {
        return new MockNetworkCommand()
                .setMsgId(getRandom().nextInt())
                .setPayload(UUID.randomUUID().toString())
                .setSenderId(roleMock.getSettings().getRoleId())
                .setSenderAddress(roleMock.getSettings().getAddress())
                .setReceiverAddress(roleMock.getSettings().getAddress());
    }

}
