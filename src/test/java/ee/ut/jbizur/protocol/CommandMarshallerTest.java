package ee.ut.jbizur.protocol;

import ee.ut.jbizur.datastore.bizur.BucketView;
import ee.ut.jbizur.network.address.MockAddress;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.bizur.AckRead_NC;
import ee.ut.jbizur.protocol.commands.common.Nack_NC;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandMarshallerTest {

    private NetworkCommand expectedNC;

    @Before
    public void setUp() {
        Map<String, String> bucketMap = new HashMap<>();
        bucketMap.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        bucketMap.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        bucketMap.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        BucketView bucketView = new BucketView()
                .setBucketMap(bucketMap)
                .setIndex(0)
                .setVerCounter(1)
                .setVerElectId(2);

        expectedNC = new AckRead_NC()
                .setBucketView(bucketView)
                .setSenderAddress(new MockAddress("sender-address"))
                .setReceiverAddress(new MockAddress("receiver-address"))
                .setMsgId("msgId")
                .setSenderId("senderId")
                .setRetryCount(5)
                .setHandled(false)
                .setTag(3)
                .setNodeType("member")
                .setPayload(new Nack_NC());
    }

    @Test
    public void testJsonSerializer() {
        CommandMarshaller marshaller = new CommandMarshaller().setSerializer(new JsonSerializer());
        marshallUnmarshall(expectedNC, marshaller);
    }

    @Test
    public void testStringSerializer() {
        CommandMarshaller marshaller = new CommandMarshaller().setSerializer(new ObjectSerializer());
        marshallUnmarshall(expectedNC, marshaller);
    }

    private void marshallUnmarshall(NetworkCommand expectedNC, CommandMarshaller marshaller) {
        NetworkCommand actualNC;

        String str = marshaller.marshall(expectedNC);
        actualNC = marshaller.unmarshall(str);
        validateCommands(expectedNC, actualNC);

        byte[] bytes = marshaller.marshall(expectedNC, byte[].class);
        actualNC = marshaller.unmarshall(bytes);
        validateCommands(expectedNC, actualNC);
    }

    private void validateCommands(NetworkCommand expectedNC, NetworkCommand actualNC) {
        String expNCStr = expectedNC.toString();
        String actNCStr = actualNC.toString();
        char[] expNCCharArr = expNCStr.toCharArray();
        char[] actNCCharArr = actNCStr.toCharArray();
        Arrays.sort(expNCCharArr);
        Arrays.sort(actNCCharArr);
        Assert.assertEquals(new String(expNCCharArr), new String(actNCCharArr));
    }

}