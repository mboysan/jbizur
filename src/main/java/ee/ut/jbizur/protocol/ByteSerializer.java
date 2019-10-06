package ee.ut.jbizur.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;

/**
 * Taken from <a href="https://stackoverflow.com/a/134918">source</a>.
 */
public class ByteSerializer implements ISerializer {

    private static final Logger logger = LoggerFactory.getLogger(ByteSerializer.class);

    @Override
    public String serializeToString(Serializable serializable) {
        byte[] bytes = serializeToBytes(serializable);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public Object deSerializeFromString(String deSerializable) {
        byte[] bytes = Base64.getDecoder().decode(deSerializable);
        return deSerializeFromBytes(bytes);
    }

    @Override
    public byte[] serializeToBytes(Serializable serializable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(serializable);
            oos.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    @Override
    public Object deSerializeFromBytes(byte[] deSerializable) {
        ObjectInputStream ois;
        Object o = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(deSerializable));
            o = ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
        return o;
    }
}
