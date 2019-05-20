package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.FuncTestConf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MockAddress;
import ee.ut.jbizur.util.IdUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static utils.TestUtils.getRandom;

public class BizurNodeTestBase {

    private static final int NODE_COUNT = FuncTestConf.get().members.size();
    BizurNode[] bizurNodes;

    private Map<String, String> expKeyVals;
    private Set<Integer> leaderDefinedBucketIndexes;

    @Before
    public void setUp() throws Exception {
        createNodes();
        registerRoles();
        startRoles();
        this.expKeyVals = new ConcurrentHashMap<>();
        this.leaderDefinedBucketIndexes = ConcurrentHashMap.newKeySet();
    }

    private void createNodes() throws UnknownHostException, InterruptedException {
        int nodeCount = FuncTestConf.get().members.size();
        Address[] addresses = new MockAddress[nodeCount];
        String[] members = new String[nodeCount];
        for (int i = 0; i < members.length; i++) {
            members[i] = FuncTestConf.get().members.get(i).id;
            addresses[i] = new MockAddress(members[i]);
        }

        bizurNodes = new BizurNode[NODE_COUNT];
        for (int i = 0; i < bizurNodes.length; i++) {
            bizurNodes[i] = BizurMockBuilder.mockBuilder()
                    .withMemberId(members[i])
                    .withMulticastEnabled(false)
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
        for (BizurNode bizurNode : bizurNodes) {
            bizurNode.shutdown();
        }
    }

    BizurNodeMock getRandomNode() {
        return getNode(-1);
    }

    BizurNodeMock getNode(int inx) {
        return (BizurNodeMock) bizurNodes[inx == -1 ? getRandom().nextInt(bizurNodes.length) : inx];
    }

    protected int hashKey(String s) {
        return IdUtils.hashKey(s, FuncTestConf.get().consensus.bizur.bucketCount);
    }

    void putExpectedKeyValue(String expKey, String expVal) {
        expKeyVals.put(expKey, expVal);
        leaderDefinedBucketIndexes.add(hashKey(expKey));
    }
    void removeExpectedKey(String expKey) {
        expKeyVals.remove(expKey);
    }
    String getExpectedValue(String expKey) {
        return expKeyVals.get(expKey);
    }
    Set<String> getExpectedKeySet() {
        return expKeyVals.keySet();
    }

    @After
    public void validateKeyValsForAllNodes() {
        for (BizurNode bizurNode : bizurNodes) {
            validateKeyVals(bizurNode);
        }
    }

    private void validateKeyVals(BizurNode byNode) {
        Set<String> actKeys = byNode.iterateKeys();
        Assert.assertEquals(getExpectedKeySet().size(), actKeys.size());
        for (String expKey : getExpectedKeySet()) {
            Assert.assertEquals(logNode(byNode, hashKey(expKey)), getExpectedValue(expKey), byNode.get(expKey));
        }
        for (String actKey : actKeys) {
            Assert.assertEquals(logNode(byNode, hashKey(actKey)), getExpectedValue(actKey), byNode.get(actKey));
        }
    }

    @After
    public void validateLocalBucketKeyVals() {
        if (expKeyVals.size() == 0) {
            leaderDefinedBucketIndexes.iterator().forEachRemaining(bIdx -> {
                BizurNode leader = findLeaderOfBucket(bIdx);
                Assert.assertEquals(logNode(leader, bIdx), 0, leader.bucketContainer.getBucket(bIdx).getKeySet().size());
            });
        } else {
            expKeyVals.forEach((expKey, expVal) -> {
                int bIdx = hashKey(expKey);
                BizurNode leader = findLeaderOfBucket(bIdx);
                Assert.assertEquals(logNode(leader, bIdx), expVal, leader.bucketContainer.getBucket(bIdx).getOp(expKey));
            });
        }
    }

    private BizurNode findLeaderOfBucket(int bucketIndex) {
        for (BizurNode bizurNode : bizurNodes) {
            if (bizurNode.bucketContainer.getBucket(bucketIndex).isLeader()) {
                return bizurNode;
            }
        }
        Assert.fail();
        return null;
    }

    String logNode(BizurNode bizurNode, int bucketIndex) {
        String log = "node=[%s], bucket=[%s], keySet=[%s]";
        return String.format(log,
                bizurNode.toString(),
                bizurNode.bucketContainer.getBucket(bucketIndex),
                bizurNode.bucketContainer.getBucket(bucketIndex).getKeySet());
    }

}
