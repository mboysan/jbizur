package ee.ut.jbizur.role;

import java.io.Serializable;

public class SerializableBucket extends Bucket<Serializable, Serializable> {
    SerializableBucket(int index) {
        super(index);
    }
}
