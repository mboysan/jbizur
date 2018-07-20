package role;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class BizurNodeCrashTest extends BizurNodeTestBase {

    private final Map<String, String> expKeyVals = new HashMap<>();

    public void setUp() throws Exception {
        super.setUp();
        Pinger.RETRY_PING_COUNT = 1;
        expKeyVals.clear();
    }

    @Test
    public void sendFailTest() {
        // elect leader as n0
        getNode(0).tryElectLeader();
        Assert.assertTrue(getNode(0).isLeader());

        // set new key-vals
        setRandomKeyVals();

        // kill n1
        getNode(1).setDead(true);

        // set new key-vals
        setRandomKeyVals();

        // revive n1
        getNode(1).setDead(false);

        // set new key-vals
        setRandomKeyVals();

        validateKeyValsForAllNodes();
    }

    @Test
    public void sendFailOnLeaderTest() {
        // elect leader as n0
        getNode(0).tryElectLeader();
        Assert.assertTrue(getNode(0).isLeader());

        // set new key-vals
        setRandomKeyVals();

        // kill n0 (leader)
        getNode(0).setDead(true);

        // set new key-vals
        setRandomKeyVals();

        // revive n0 (leader)
        getNode(0).setDead(false);

        // set new key-vals
//        setRandomKeyVals();
        setRandomKeyVals(1);
        setRandomKeyVals(2);
        setRandomKeyVals(0);

//        validateKeyValsForAllNodes();
        validateKeyVals();
    }

    private void setRandomKeyVals() {
        for (BizurNode bizurNode : bizurNodes) {
            setRandomKeyVals(bizurNode);
        }
    }

    private void setRandomKeyVals(int byNodeId) {
        setRandomKeyVals(getNode(byNodeId));
    }

    private void setRandomKeyVals(BizurNode byNode) {
        String testKey = UUID.randomUUID().toString();
        String expVal = UUID.randomUUID().toString();
        if(byNode instanceof BizurNodeMock && !((BizurNodeMock) byNode).isDead()){
            byNode.set(testKey, expVal);
            expKeyVals.put(testKey, expVal);
        }
    }

    private void validateKeyValsForAllNodes() {
        for (BizurNode bizurNode : bizurNodes) {
            validateKeyVals(bizurNode);
        }
    }

    private void validateKeyVals() {
        validateKeyVals(getRandomNode());
    }

    private void validateKeyVals(int nodeId) {
        validateKeyVals(getNode(nodeId));
    }

    private void validateKeyVals(BizurNode byNode) {
        StringBuilder actKeyStr = new StringBuilder();
        StringBuilder actValStr = new StringBuilder();
        Set<String> keys = byNode.iterateKeys();
        for (String key : keys) {
            actKeyStr.append(key);
            actValStr.append(byNode.get(key));
        }

        StringBuilder expKeyStr = new StringBuilder();
        StringBuilder expValStr = new StringBuilder();
        Set<String> expKeys = expKeyVals.keySet();
        for (String expKey : expKeys) {
            expKeyStr.append(expKey);
            expValStr.append(expKeyVals.get(expKey));
        }

        Assert.assertEquals(sortString(expKeyStr.toString()), sortString(actKeyStr.toString()));
        Assert.assertEquals(sortString(expValStr.toString()), sortString(actValStr.toString()));
    }

    // Method to sort a string alphabetically
    public static String sortString(String inputString) {
        char tempArray[] = inputString.toCharArray();
        Arrays.sort(tempArray);
        return new String(tempArray);
    }

}
