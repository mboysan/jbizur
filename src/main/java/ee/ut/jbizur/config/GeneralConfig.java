package ee.ut.jbizur.config;

import ee.ut.jbizur.network.messenger.AbstractClient;
import ee.ut.jbizur.network.messenger.AbstractServer;
import ee.ut.jbizur.network.messenger.tcp.custom.BlockingClientImpl;
import ee.ut.jbizur.network.messenger.tcp.custom.BlockingServerImpl;
import ee.ut.jbizur.protocol.ISerializer;
import ee.ut.jbizur.protocol.ByteSerializer;
import org.pmw.tinylog.Logger;

public class GeneralConfig {

    public static Class<? extends AbstractServer> getServerClass() {
        try {
            return (Class<? extends AbstractServer>) Class.forName(PropertiesLoader.getString("protocol.server.class"));
        } catch (Exception e) {
            Logger.warn("defaulting to " + ByteSerializer.class + ", reason: " + e);
        }
        return BlockingServerImpl.class;
    }

    public static Class<? extends AbstractClient> getClientClass() {
        try {
            return (Class<? extends AbstractClient>) Class.forName(PropertiesLoader.getString("protocol.client.class"));
        } catch (Exception e) {
            Logger.warn("defaulting to " + ByteSerializer.class + ", reason: " + e);
        }
        return BlockingClientImpl.class;
    }

    public static Class<? extends ISerializer> getProtocolSerializerClass() {
        try {
            return (Class<? extends ISerializer>) Class.forName(PropertiesLoader.getString("protocol.serializer.class"));
        } catch (Exception e) {
            Logger.warn("defaulting to " + ByteSerializer.class + ", reason: " + e);
        }
        return ByteSerializer.class;
    }

    public static SerializationType getTCPSerializationType() {
        switch (PropertiesLoader.getString("protocol.tcp.sendrecv.type", "OBJECT").toUpperCase()) {
            case "OBJECT" : return SerializationType.OBJECT;
            case "BYTE": return SerializationType.BYTE;
            case "STRING": return SerializationType.STRING;
            case "JSON": return SerializationType.JSON;
        }
        throw new IllegalArgumentException("serialization type could not be determined.");
    }

    public static boolean tcpKeepAlive() {
        return PropertiesLoader.getBoolean("protocol.tcp.keepalive", false);
    }

    public enum SerializationType {
        BYTE, OBJECT, STRING, JSON
    }
}
