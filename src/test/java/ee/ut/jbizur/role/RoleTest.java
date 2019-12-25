package ee.ut.jbizur.role;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.Ping_NC;
import ee.ut.jbizur.protocol.commands.nc.ping.Pong_NC;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import utils.MockUtils;
import utils.MultiThreadExecutor;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertTrue;

public class RoleTest {
    static {
        Conf.setConfigFromResources("RoleUT.conf");
    }

    private Role role1;
    private Role role2;

    @Before
    public void setUp() throws Exception {
        Address role1Addr = MockUtils.mockAddress("role1");
        Address role2Addr = MockUtils.mockAddress("role2");

        RoleSettings rs1 = new RoleSettings()
                .setAddress(role1Addr)
                .setMemberAddresses(new HashSet<>(Collections.singletonList(role2Addr)));
        RoleSettings rs2 = new RoleSettings()
                .setAddress(role2Addr)
                .setMemberAddresses(new HashSet<>(Collections.singletonList(role1Addr)));

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
    }

    @After
    public void tearDown() throws Exception {
        role1.close();
        role2.close();
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
            mte.execute(() -> {
                testReqResp(corrId);
            });
        }
        mte.endExecution();
    }

    @Test
    public void testReqResp() {
        testReqResp(1);
    }

    private void testReqResp(int corrId) {
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
            mte.execute(this::testPing);
        }
        mte.endExecution();
    }

    @Test
    public void testPing() {
        Assert.assertTrue(role1.ping(role2.getSettings().getAddress()));
    }
}