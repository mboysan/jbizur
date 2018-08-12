package ee.ut.jbizur.protocol;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import ee.ut.jbizur.protocol.commands.NetworkCommand;

import java.nio.charset.StandardCharsets;

/**
 * A simple marshaller used to marshall/unmarshall commands.
 */
public class CommandMarshaller {

    private XStream xstream;

    public CommandMarshaller() {
        xstream = new XStream(new JettisonMappedXmlDriver());
//        xstream.setMode(XStream.NO_REFERENCES);
    }

    /**
     * Marshalls the obj into String.
     * @param obj command to marshall into String
     * @return obj marshalled into String.
     */
    public String marshall(NetworkCommand obj) {
        return xstream.toXML(obj);
    }

    /**
     * @param obj command to marshall into requested type.
     * @param type class type to marshall the command for.
     * @param <T>  type to marshall the command for.
     * @return obj marshalled into requested type.
     */
    public <T> T marshall(NetworkCommand obj, Class<T> type) {
        String jsonStr = marshall(obj);
        if(type.isAssignableFrom(String.class)){
            return (T) jsonStr;
        } else if(type.isAssignableFrom(byte[].class)){
            return (T) jsonStr.getBytes(StandardCharsets.UTF_8);
        } else if(type.isAssignableFrom(char[].class)){
            return (T) jsonStr.toCharArray();
        }
        return null;
    }

    /**
     * @param commandAsJson command as json string.
     * @return command unmarshalled into its associated {@link NetworkCommand}.
     */
    public NetworkCommand unmarshall(String commandAsJson) {
        return (NetworkCommand) xstream.fromXML(commandAsJson);
    }
}
