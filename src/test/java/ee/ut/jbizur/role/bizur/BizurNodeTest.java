package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.common.ResourceCloser;
import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.ping.Ping_NC;
import ee.ut.jbizur.protocol.commands.nc.ping.Pong_NC;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import utils.MockUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BizurNodeTest implements ResourceCloser {
    static {
        Conf.setConfigFromResources("BizurUT.conf");
    }

    private BizurNode member1;
    private BizurNode member2;
    private BizurNode member3;

    private BizurClient client1;

    @Before
    public void setUp() throws Exception {
        Address m1Addr = MockUtils.mockAddress("m1");
        Address m2Addr = MockUtils.mockAddress("m2");
        Address m3Addr = MockUtils.mockAddress("m3");
        Set<Address> memberAddresses = new HashSet<>(Arrays.asList(m1Addr, m2Addr, m3Addr));

        member1 = createMember(m1Addr, memberAddresses);
        member2 = createMember(m2Addr, memberAddresses);
        member3 = createMember(m3Addr, memberAddresses);

        CompletableFuture<Void> m1future = member1.start();
        CompletableFuture<Void> m2future = member2.start();
        CompletableFuture<Void> m3future = member3.start();
        m1future.join();
        m2future.join();
        m3future.join();

        member1.checkReady();
        member2.checkReady();
        member3.checkReady();
    }

    private BizurNode createMember(Address selfAddress, Set<Address> otherMemberAddresses) throws IOException {
        return BizurBuilder.builder()
                .withMemberId(selfAddress.resolveAddressId())
                .withAddress(selfAddress)
                .withMulticastEnabled(false)
                .withMemberAddresses(otherMemberAddresses)
                .build();
    }

    @After
    public void tearDown() {
        closeResources(member1, member2, member3);
        closeResources(client1);
    }

    @Test
    public void testRoute() {
        NetworkCommand ping = new Ping_NC()
                .setCorrelationId(1)
                .setSenderAddress(member1.getSettings().getAddress())
                .setReceiverAddress(member2.getSettings().getAddress());

        NetworkCommand pong = member1.route(ping);
        Assert.assertTrue(pong instanceof Pong_NC);
        Assert.assertEquals(ping.getCorrelationId(), pong.getCorrelationId());
    }
}