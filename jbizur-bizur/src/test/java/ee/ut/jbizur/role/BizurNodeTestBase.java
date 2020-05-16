package ee.ut.jbizur.role;

import ee.ut.jbizur.common.util.IdUtil;
import ee.ut.jbizur.common.util.RngUtil;
import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.util.MockUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
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

    private static final Logger logger = LoggerFactory.getLogger(BizurNodeTestBase.class);

    static final String MAP = "test-map";

    private static final int NODE_COUNT = CoreConf.get().members.size();
    BizurNode[] bizurNodes;

    private Map<Serializable, Serializable> expKeyVals;
    private Set<Integer> leaderDefinedBucketIndexes;

    @Before
    public void setUp() throws IOException {
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

    void electBucketLeaders() {
        int bucketCount = CoreConf.get().consensus.bizur.bucketCount;
        for (int i = 0; i < bucketCount; i++) {
            bizurNodes[i % bizurNodes.length].getMap(MAP).startElection(i);
        }
    }

    BizurNode getRandomNode() {
        return getNode(-1);
    }

    BizurNode getNode(int inx) {
        return bizurNodes[inx == -1 ? RngUtil.nextInt(bizurNodes.length) : inx];
    }

    protected int hashKey(Serializable s) {
        return IdUtil.hashKey(s, CoreConf.get().consensus.bizur.bucketCount);
    }

    protected int hashKey(String s) {
        return IdUtil.hashKey(s, CoreConf.get().consensus.bizur.bucketCount);
    }

    void putExpectedKeyValue(Serializable expKey, Serializable expVal) {
        if (expKeyVals.get(expKey) != null) {
            logger.warn("key exists, is this intended? key={}", expKey);
        }
        expKeyVals.put(expKey, expVal);
        leaderDefinedBucketIndexes.add(hashKey(expKey));
    }

    void removeExpectedKey(Serializable expKey) {
        expKeyVals.remove(expKey);
    }

    Serializable getExpectedValue(Serializable expKey) {
        return expKeyVals.get(expKey);
    }

    Set<Serializable> getExpectedKeySet() {
        return expKeyVals.keySet();
    }

    void assertKeySetEquals(BizurNode node) {
        Set<Serializable> actKeys = node.getMap(MAP).keySet();
        Assert.assertEquals(getExpectedKeySet().size(), actKeys.size());
        for (Serializable expKey : getExpectedKeySet()) {
            Assert.assertEquals(getExpectedValue(expKey), node.getMap(MAP).get(expKey));
        }
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
        Set<Serializable> actKeys = byNode.getMap(MAP).keySet();
        Assert.assertEquals(byNode.logMsg("expected keyset and actual keyset size don't match"),
                getExpectedKeySet().size(), actKeys.size());
        for (Serializable expKey : getExpectedKeySet()) {
            Assert.assertEquals(logNode(byNode, hashKey(expKey)), getExpectedValue(expKey), byNode.getMap(MAP).get(expKey));
        }
        for (Serializable actKey : actKeys) {
            Assert.assertEquals(logNode(byNode, hashKey(actKey)), getExpectedValue(actKey), byNode.getMap(MAP).get(actKey));
        }
    }

    private void validateLocalBucketKeyVals() {
        if (expKeyVals.size() == 0) {
            leaderDefinedBucketIndexes.iterator().forEachRemaining(bIdx -> {
                BizurNode leader = findLeaderOfBucket(bIdx);
                int actKeySetSize = execOnBucket(leader.getMap(MAP).bucketContainer, bIdx, (b) -> b.getKeySetOp().size());
                Assert.assertEquals(logNode(leader, bIdx), 0, actKeySetSize);
            });
        } else {
            expKeyVals.forEach((expKey, expVal) -> {
                int bIdx = hashKey(expKey);
                BizurNode leader = findLeaderOfBucket(bIdx);
                Serializable actKey = execOnBucket(leader.getMap(MAP).bucketContainer, bIdx, (b) -> b.getOp(expKey));
                Assert.assertEquals(logNode(leader, bIdx), expVal, actKey);
            });
        }
    }

    private BizurNode findLeaderOfBucket(int bucketIndex) {
        for (BizurNode bizurNode : bizurNodes) {
            if (execOnBucket(bizurNode.getMap(MAP).bucketContainer, bucketIndex, Bucket::isLeader)) {
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
                execOnBucket(bizurNode.getMap(MAP).bucketContainer, bucketIndex, b -> b),
                execOnBucket(bizurNode.getMap(MAP).bucketContainer, bucketIndex, b -> b.getKeySetOp())
        );
    }

    public void tearDown() {
        for (BizurNode bizurNode : bizurNodes) {
            bizurNode.close();
        }
    }

    static <R> R execOnBucket(BucketContainer bucketContainer, int index, Function<SerializableBucket, R> function) {
        SerializableBucket bucket = bucketContainer.tryAndLockBucket(index);
        if (bucket != null) {
            try {
                return function.apply(bucket);
            } finally {
                bucket.unlock();
            }
        }
        Assert.fail();
        return null;
    }
}
