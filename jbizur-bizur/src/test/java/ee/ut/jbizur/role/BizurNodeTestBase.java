package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.util.MockUtil;
import ee.ut.jbizur.common.util.RngUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BizurNodeTestBase {
    static {
        CoreConf.setConfig("BizurUT.conf");
    }

    private static final int NODE_COUNT = CoreConf.get().members.size();
    BizurNode[] bizurNodes;

    private Map<String, String> expKeyVals;
    private Set<Integer> leaderDefinedBucketIndexes;

    @Before
    public void setUp() throws Exception {
        createNodes();
        startRoles();
        this.expKeyVals = new ConcurrentHashMap<>();
        this.leaderDefinedBucketIndexes = ConcurrentHashMap.newKeySet();
    }

    private void createNodes() throws IOException {
        Address[] addresses = new Address[NODE_COUNT];
        String[] members = new String[NODE_COUNT];
        for (int i = 0; i < members.length; i++) {
            members[i] = CoreConf.get().members.get(i).id;
            addresses[i] = MockUtil.mockAddress(members[i]);
        }

        bizurNodes = new BizurNode[NODE_COUNT];
        for (int i = 0; i < bizurNodes.length; i++) {
            bizurNodes[i] = BizurBuilder.builder()
                    .withMemberId(members[i])
                    .withMulticastEnabled(false)
                    .withAddress(addresses[i])
                    .withMemberAddresses(new HashSet<>(Arrays.asList(addresses)))
                    .build();
        }
    }

    private void startRoles() {
        CompletableFuture<Void>[] futures = new CompletableFuture[bizurNodes.length];
        for (int i = 0; i < bizurNodes.length; i++) {
            futures[i] = bizurNodes[i].start();
        }
        for (CompletableFuture<Void> future : futures) {
            future.join();
        }
    }

    BizurNode getRandomNode() {
        return getNode(-1);
    }

    BizurNode getNode(int inx) {
        return bizurNodes[inx == -1 ? RngUtil.nextInt(bizurNodes.length) : inx];
    }

    protected int hashKey(String s) {
        return IdUtil.hashKey(s, CoreConf.get().consensus.bizur.bucketCount);
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
    public void postValidationsAndTearDown() {
        validateKeyValsForAllNodes();
//        validateLocalBucketKeyVals();
        tearDown();
    }

    private void validateKeyValsForAllNodes() {
        for (BizurNode bizurNode : bizurNodes) {
            validateKeyVals(bizurNode);
        }
    }

    private void validateKeyVals(BizurNode byNode) {
        Set<String> actKeys = byNode.iterateKeys();
        Assert.assertEquals(byNode.logMsg("expected keyset and actual keyset size don't match"),
                getExpectedKeySet().size(), actKeys.size());
        for (String expKey : getExpectedKeySet()) {
            Assert.assertEquals(logNode(byNode, hashKey(expKey)), getExpectedValue(expKey), byNode.get(expKey));
        }
        for (String actKey : actKeys) {
            Assert.assertEquals(logNode(byNode, hashKey(actKey)), getExpectedValue(actKey), byNode.get(actKey));
        }
    }

    private void validateLocalBucketKeyVals() {
        if (expKeyVals.size() == 0) {
            leaderDefinedBucketIndexes.iterator().forEachRemaining(bIdx -> {
                BizurNode leader = findLeaderOfBucket(bIdx);
                int actKeySetSize = execOnBucket(leader.bucketContainer, bIdx, (b) -> b.getKeySetOp().size());
                Assert.assertEquals(logNode(leader, bIdx), 0, actKeySetSize);
            });
        } else {
            expKeyVals.forEach((expKey, expVal) -> {
                int bIdx = hashKey(expKey);
                BizurNode leader = findLeaderOfBucket(bIdx);
                String actKey = execOnBucket(leader.bucketContainer, bIdx, (b) -> b.getOp(expKey));
                Assert.assertEquals(logNode(leader, bIdx), expVal, actKey);
            });
        }
    }

    private BizurNode findLeaderOfBucket(int bucketIndex) {
        for (BizurNode bizurNode : bizurNodes) {
            if (execOnBucket(bizurNode.bucketContainer, bucketIndex, Bucket::isLeader)) {
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
                execOnBucket(bizurNode.bucketContainer, bucketIndex, b -> b),
                execOnBucket(bizurNode.bucketContainer, bucketIndex, b -> b.getKeySetOp())
        );
    }

    public void tearDown() {
        for (BizurNode bizurNode : bizurNodes) {
            bizurNode.close();
        }
    }

    static <R> R execOnBucket(BucketContainer bucketContainer, int index, Function<Bucket, R> function) {
        Bucket bucket = bucketContainer.lockAndGetBucket(index);
        try {
            return function.apply(bucket);
        } finally {
            bucketContainer.unlockBucket(index);
        }
    }
}
