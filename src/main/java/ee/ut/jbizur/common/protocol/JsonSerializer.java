package ee.ut.jbizur.common.protocol;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class JsonSerializer implements ISerializer {

    private XStream xstream;

    public JsonSerializer() {
        xstream = new XStream(new JettisonMappedXmlDriver());
//        xstream.setMode(XStream.NO_REFERENCES);
    }

    @Override
    public String serializeToString(Serializable serializable) {
        return xstream.toXML(serializable);
    }

    @Override
    public Object deSerializeFromString(String deSerializable) {
        return xstream.fromXML(deSerializable);
    }

    @Override
    public byte[] serializeToBytes(Serializable serializable) {
        return serializeToString(serializable).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object deSerializeFromBytes(byte[] deSerializable) {
        return deSerializeFromString(new String(deSerializable, StandardCharsets.UTF_8));
    }
}
