package ee.ut.jbizur.config;

import ee.ut.jbizur.protocol.ISerializer;
import ee.ut.jbizur.protocol.ByteSerializer;
import org.pmw.tinylog.Logger;

public class GeneralConfig {
    public static Class<? extends ISerializer> getProtocolSerializerClass() {
        try {
            return (Class<? extends ISerializer>) Class.forName(PropertiesLoader.getString("protocol.serializer.class"));
        } catch (Exception e) {
            Logger.warn(e);
        }
        return ByteSerializer.class;
    }

    public static SerializationType getTCPSerializationType() {
        switch (PropertiesLoader.getString("protocol.tcp.sendrecv.type").toUpperCase()) {
            case "OBJECT" : return SerializationType.OBJECT;
            case "BYTE": return SerializationType.BYTE;
        }
        throw new IllegalArgumentException("serialization type could not be determined.");
    }

    public enum SerializationType {
        BYTE, OBJECT
    }
}
