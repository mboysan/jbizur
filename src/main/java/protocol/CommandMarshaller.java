package protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import protocol.commands.NetworkCommand;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A simple marshaller used to marshall/unmarshall commands.
 */
public class CommandMarshaller {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Marshalls the obj into String.
     * @param obj command to marshall into String
     * @return obj marshalled into String.
     * @throws JsonProcessingException in case of json parsing exceptions.
     */
    public String marshall(NetworkCommand obj) throws JsonProcessingException {
        String objAsJson = mapper.writeValueAsString(obj);
        JSONObject jsonObject = new JSONObject(objAsJson);
        jsonObject.put("_type", obj.getClass().getName());
        return jsonObject.toString();
    }

    /**
     * @param obj command to marshall into requested type.
     * @param type class type to marshall the command for.
     * @param <T>  type to marshall the command for.
     * @return obj marshalled into requested type.
     * @throws JsonProcessingException in case of json parsing exceptions.
     */
    public <T> T marshall(NetworkCommand obj, Class<T> type) throws JsonProcessingException {
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
     * @throws IOException in case of json related operations.
     */
    public NetworkCommand unmarshall(String commandAsJson) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject(commandAsJson);
        try{
            Class clazz = Class.forName(jsonObject.getString("_type"));
            jsonObject.remove("_type");
            return mapper.readValue(jsonObject.toString(), (Class<NetworkCommand>) clazz);
        }catch (ClassNotFoundException | ClassCastException e){
            throw new IOException(e);
        }
    }
}
