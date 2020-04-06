package ee.ut.jbizur.common.protocol;

import java.io.Serializable;

public interface ISerializer {
    String serializeToString(Serializable serializable);
    byte[] serializeToBytes(Serializable serializable);
    Object deSerializeFromString(String deSerializable);
    Object deSerializeFromBytes(byte[] deSerializable);
}
