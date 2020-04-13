package ee.ut.jbizur.util;

import java.util.UUID;

public class TestUtil {

    public static String getRandomString() {
        return getRandomString(8);
    }

    public static String getRandomString(int length) {
        String r = UUID.randomUUID().toString();
        if (length > r.length()) {
            throw new IllegalArgumentException("length cannot be greater than " + r.length());
        }
        return r.substring(0, length);
    }
}
