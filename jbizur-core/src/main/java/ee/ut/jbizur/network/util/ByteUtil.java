package ee.ut.jbizur.network.util;

import java.nio.ByteBuffer;

public final class ByteUtil {

    public static byte[] prependMessageLengthTo(byte[] msg) {
        byte[] msgLength = ByteBuffer.allocate(4).putInt(msg.length).array();
        byte[] msgWithLength = new byte[msg.length + 4];
        System.arraycopy(msg, 0, msgWithLength, 4, msg.length);
        msgWithLength[0] = msgLength[0];
        msgWithLength[1] = msgLength[1];
        msgWithLength[2] = msgLength[2];
        msgWithLength[3] = msgLength[3];
        return msgWithLength;
    }

    public static byte[] extractActualMessage(byte[] receivedMsg) {
        int size = ByteBuffer.wrap(receivedMsg).getInt();
        byte[] actualMsg = new byte[size];
        System.arraycopy(receivedMsg, 4, actualMsg, 0, size);
        return actualMsg;
    }

}
