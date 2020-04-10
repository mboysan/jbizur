package ee.ut.jbizur;

import ee.ut.jbizur.protocol.ByteSerializer;
import ee.ut.jbizur.protocol.CommandMarshaller;
import ee.ut.jbizur.protocol.JsonSerializer;
import ee.ut.jbizur.protocol.commands.net.Nack_NC;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import ee.ut.jbizur.protocol.commands.net.Ping_NC;
import ee.ut.jbizur.util.MockUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class CommandMarshallerTest {

    private NetworkCommand expectedNC;

    @Before
    public void setUp() {
        expectedNC = new Ping_NC()
                .setSenderAddress(MockUtil.mockAddress("sender-address"))
                .setReceiverAddress(MockUtil.mockAddress("receiver-address"))
                .setMsgId(12345)
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
        CommandMarshaller marshaller = new CommandMarshaller().setSerializer(new ByteSerializer());
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