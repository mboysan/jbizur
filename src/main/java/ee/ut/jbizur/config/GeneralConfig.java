package ee.ut.jbizur.config;

import ee.ut.jbizur.protocol.ISerializer;
import ee.ut.jbizur.protocol.ObjectSerializer;
import org.pmw.tinylog.Logger;

public class GeneralConfig {
    public static Class<? extends ISerializer> getProtocolSerializerClass() {
        try {
            return (Class<? extends ISerializer>) Class.forName(PropertiesLoader.getString("protocol.serializer.class"));
        } catch (Exception e) {
            Logger.warn(e);
        }
        return ObjectSerializer.class;
    }
}
