package ee.ut.jbizur.clientservertest;

import ee.ut.jbizur.protocol.commands.MockNetworkCommand;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.RoleMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import utils.RunnerWithExceptionCatcher;

import java.util.UUID;

@Ignore
public class MsgSendRecvTest {

    private RoleMock roleMock;

    @Before
    public void setUp() throws Exception {
        this.roleMock = new RoleMock(null);
    }

    @Test
    public void testMessageSendRecv() throws Throwable {
        int testCount = 1000;
        RunnerWithExceptionCatcher runner = new RunnerWithExceptionCatcher(testCount);
        for (int i = 0; i < testCount; i++) {
            runner.execute(() -> {
                roleMock.getMessageSender().send(generateCommand());
            });
        }

        Thread.sleep(5000);

        Assert.assertEquals(testCount, roleMock.receivedCommandsMap.size());
    }

    protected NetworkCommand generateCommand() {
        return new MockNetworkCommand()
                .setMsgId(UUID.randomUUID().toString())
                .setPayload(UUID.randomUUID().toString())
                .setSenderAddress(roleMock.getSettings().getAddress())
                .setReceiverAddress(roleMock.getSettings().getAddress());
    }

}
