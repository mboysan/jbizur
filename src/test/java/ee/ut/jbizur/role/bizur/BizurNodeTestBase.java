package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.NodeTestConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MockAddress;
import ee.ut.jbizur.network.address.MockMulticastAddress;
import org.junit.After;
import org.junit.Before;
import org.pmw.tinylog.Logger;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class BizurNodeTestBase {

    protected Random random = getRandom();

    protected static final int NODE_COUNT = 3;
    protected BizurNode[] bizurNodes;

    @Before
    public void setUp() throws Exception {
        createNodes();
        registerRoles();
        startRoles();
    }

    private void createNodes() throws UnknownHostException, InterruptedException {
        int nodeCount = NodeTestConfig.getMemberCount();
        Address[] addresses = new MockAddress[nodeCount];
        String[] members = new String[nodeCount];
        for (int i = 0; i < members.length; i++) {
            members[i] = NodeTestConfig.getMemberId(i);
            addresses[i] = new MockAddress(members[i]);
        }

        bizurNodes = new BizurNode[NODE_COUNT];
        for (int i = 0; i < bizurNodes.length; i++) {
            bizurNodes[i] = BizurMockBuilder.mockBuilder()
                    .withMemberId(members[i])
                    .withMulticastAddress(new MockMulticastAddress("", 0))
                    .withAddress(addresses[i])
                    .withMemberAddresses(new HashSet<>(Arrays.asList(addresses)))
                    .build();
        }
    }

    private void registerRoles() {
        for (BizurNode bizurNode : bizurNodes) {
            ((BizurNodeMock) bizurNode).registerRoles(bizurNodes);
        }
    }

    private void startRoles() {
        CompletableFuture[] futures = new CompletableFuture[bizurNodes.length];
        for (int i = 0; i < bizurNodes.length; i++) {
            futures[i] = bizurNodes[i].start();
        }
        for (CompletableFuture future : futures) {
            future.join();
        }
    }

    @After
    public void tearDown() {
    }

    protected Random getRandom() {
        long seed = System.currentTimeMillis();
        return getRandom(seed);
    }

    protected Random getRandom(long seed) {
        Logger.info("Seed: " + seed);
        return new Random(seed);
    }

    protected BizurNodeMock getRandomNode() {
        return getNode(-1);
    }

    protected BizurNodeMock getNode(int inx) {
        return (BizurNodeMock) bizurNodes[inx == -1 ? random.nextInt(bizurNodes.length) : inx];
    }

}
