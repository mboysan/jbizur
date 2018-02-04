package protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import protocol.commands.GenericCommand;
import protocol.commands.ICommand;

import java.io.IOException;

public class CommandMarshaller {

    private final ObjectMapper mapper = new ObjectMapper();

    public String marshall(ICommand obj) throws JsonProcessingException {
        String objAsJson = mapper.writeValueAsString(obj);
        JSONObject jsonObject = new JSONObject(objAsJson);
        jsonObject.put("_type", obj.getClass().getName());
        return jsonObject.toString();
    }

    public ICommand unmarshall(String commandAsJson) throws IOException {
        JSONObject jsonObject = new JSONObject(commandAsJson);
        try{
            Class clazz = Class.forName(jsonObject.getString("_type"));
            jsonObject.remove("_type");
            return mapper.readValue(jsonObject.toString(), (Class<ICommand>) clazz);
        }catch (ClassNotFoundException | ClassCastException e){
            throw new IOException(e);
        }
    }
}
