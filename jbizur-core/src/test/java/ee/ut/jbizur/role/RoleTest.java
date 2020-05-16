package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.commands.intl.InternalCommand;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import ee.ut.jbizur.protocol.commands.net.Ping_NC;
import ee.ut.jbizur.protocol.commands.net.Pong_NC;
import ee.ut.jbizur.util.MockUtil;
import ee.ut.jbizur.util.MultiThreadExecutor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

import static ee.ut.jbizur.common.util.LambdaUtil.runnable;
import static org.junit.Assert.assertTrue;

public class RoleTest {
    static {
        CoreConf.set("RoleUT.conf");
    }

    private Role role1;
    private Role role2;
    private Role role3;

    @Before
    public void setUp() throws Exception {
        Address role1Addr = MockUtil.mockAddress("role1");
        Address role2Addr = MockUtil.mockAddress("role2");
        Address role3Addr = MockUtil.mockAddress("role3");
        Set<Address> memberAddresses = new HashSet<>(){{
            add(role1Addr);
            add(role2Addr);
            add(role3Addr);
        }};

        RoleSettings rs1 = new RoleSettings()
                .setAddress(role1Addr)
                .setMemberAddresses(memberAddresses);
        RoleSettings rs2 = new RoleSettings()
                .setAddress(role2Addr)
                .setMemberAddresses(memberAddresses);
        RoleSettings rs3 = new RoleSettings()
                .setAddress(role3Addr)
                .setMemberAddresses(memberAddresses);

        role1 = new Role(rs1) {
            @Override
            protected void handle(InternalCommand ic) {
            }
            @Override
            public CompletableFuture<Void> start() {
                return null;
            }
        };

        role2 = new Role(rs2) {
            @Override
            protected void handle(InternalCommand ic) {
            }
            @Override
            public CompletableFuture<Void> start() {
                return null;
            }
        };

        role3 = new Role(rs3) {
            @Override
            protected void handle(InternalCommand ic) {
            }
            @Override
            public CompletableFuture<Void> start() {
                return null;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        role1.close();
        role2.close();
        role3.close();
    }

    @Test
    public void testPubSubMultiThreaded() throws ExecutionException, InterruptedException {
        MultiThreadExecutor mte = new MultiThreadExecutor();
        for (int i = 0; i < 100; i++) {
            int corrId = i+1;
            mte.execute(() -> testPubSub(corrId));
        }
        mte.endExecution();
    }

    @Test
    public void testPubSub() {
        testPubSub(1);
    }

    private void testPubSub(int corrId) {
        BooleanSupplier isComplete = role1.subscribe(corrId, cmd -> cmd instanceof Pong_NC);
        role1.publish(() ->
                new Ping_NC()
                        .setCorrelationId(corrId)
                        .setSenderAddress(role1.getSettings().getAddress())
                        .setReceiverAddress(role2.getSettings().getAddress())
        );
        Assert.assertTrue(isComplete.getAsBoolean());
    }

    @Test
    public void testReqRespMultiThreaded() throws ExecutionException, InterruptedException {
        MultiThreadExecutor mte = new MultiThreadExecutor();
        for (int i = 0; i < 100; i++) {
            int corrId = i;
            mte.execute(runnable(() -> testReqResp(corrId)));
        }
        mte.endExecution();
    }

    @Test
    public void testReqResp() throws IOException {
        testReqResp(1);
    }

    private void testReqResp(int corrId) throws IOException {
        NetworkCommand ping = new Ping_NC()
                .setCorrelationId(corrId)
                .setSenderAddress(role1.getSettings().getAddress())
                .setReceiverAddress(role2.getSettings().getAddress());
        BooleanSupplier isComplete = role1.receive(ping.getCorrelationId(), resp -> assertTrue(resp instanceof Pong_NC));
        role1.send(ping);
        Assert.assertTrue(isComplete.getAsBoolean());
    }

    @Test
    public void testPingMultiThreaded() throws ExecutionException, InterruptedException {
        MultiThreadExecutor mte = new MultiThreadExecutor();
        for (int i = 0; i < 100; i++) {
            mte.execute(runnable(this::testPing));
        }
        mte.endExecution();
    }

    @Test
    public void testPing() throws IOException {
        Assert.assertTrue(role1.ping(role2.getSettings().getAddress()));
    }
}