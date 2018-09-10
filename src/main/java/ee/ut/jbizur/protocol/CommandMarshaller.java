package ee.ut.jbizur.protocol;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import org.pmw.tinylog.Logger;

/**
 * A simple marshaller used to marshall/unmarshall commands.
 */
public class CommandMarshaller {

    private ISerializer serializer;

    public CommandMarshaller() {
        try {
            serializer = GeneralConfig.getProtocolSerializerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Logger.warn(e, "could not create serializer from properties file, defaulting to " + ObjectSerializer.class.getSimpleName());
            serializer = new ObjectSerializer();
        }
    }

    public CommandMarshaller setSerializer(ISerializer serializer) {
        this.serializer = serializer;
        return this;
    }

    /**
     * Marshalls the obj into String.
     * @param obj command to marshall into String
     * @return obj marshalled into String.
     */
    public String marshall(NetworkCommand obj) {
        return marshall(obj, String.class);
    }

    /**
     * @param obj command to marshall into requested type.
     * @param type class type to marshall the command for.
     * @param <T>  type to marshall the command for.
     * @return obj marshalled into requested type.
     */
    public <T> T marshall(NetworkCommand obj, Class<T> type) {
        if(type.isAssignableFrom(String.class)){
            return (T) serializer.serializeToString(obj);
        } else if(type.isAssignableFrom(byte[].class)){
            return (T) serializer.serializeToBytes(obj);
        }
        return null;
    }

    /**
     * @param commandAsStr command as json string.
     * @return command unmarshalled into its associated {@link NetworkCommand}.
     */
    public NetworkCommand unmarshall(String commandAsStr) {
        return (NetworkCommand) serializer.deSerializeFromString(commandAsStr);
    }

    public NetworkCommand unmarshall(byte[] commandAsBytes) {
        return (NetworkCommand) serializer.deSerializeFromBytes(commandAsBytes);
    }
}
