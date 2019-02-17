package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.protocol.commands.MockNetworkCommand;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.RoleMock;
import ee.ut.jbizur.role.RoleSettings;
import org.junit.*;
import org.pmw.tinylog.Logger;
import utils.RunnerWithExceptionCatcher;

import java.util.Random;
import java.util.UUID;

@Ignore
public class MsgSendRecvTest {
    Random random = getRandom();

    private Random getRandom() {
        long seed = System.currentTimeMillis();
        return getRandom(seed);
    }

    private Random getRandom(long seed) {
        Logger.info("Seed: " + seed);
        return new Random(seed);
    }

    private RoleMock roleMock;

    @Before
    public void setUp() throws Exception {
        roleMock = new RoleMock(new RoleSettings());
        roleMock.receivedCommandsMap.clear();
    }

    @After
    public void tearDown() {
        roleMock.shutdown();
    }

    @Test
    public void testSimpleMessageSendRecv() throws Throwable {
        int testCount = 1000;
        for (int i = 0; i < testCount; i++) {
            roleMock.getMessageProcessor().getClient().send(generateCommand());
        }

        Thread.sleep(5000);

        Assert.assertEquals(testCount, roleMock.receivedCommandsMap.size());
    }

    @Test
    public void testMultithreadMessageSendRecv() throws Throwable {
        int testCount = 1000;
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                roleMock.getMessageProcessor().getClient().send(generateCommand());
            });
        }

        runner.awaitCompletion();
        runner.throwAnyCaughtException();
        Thread.sleep(5000);

        Assert.assertEquals(testCount, roleMock.receivedCommandsMap.size());
    }

    protected NetworkCommand generateCommand() {
        return new MockNetworkCommand()
                .setMsgId(random.nextInt())
                .setPayload(UUID.randomUUID().toString())
                .setSenderId(roleMock.getSettings().getRoleId())
                .setSenderAddress(roleMock.getSettings().getAddress())
                .setReceiverAddress(roleMock.getSettings().getAddress());
    }

}
