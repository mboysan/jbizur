package ee.ut.jbizur.role;

import java.io.Serializable;

public class BizurException extends Exception implements Serializable {
    BizurException(String who, String message, int index) {
        this(who + " " + message + ", index=" + index);
    }

    public BizurException(Throwable cause) {
        super(cause);
    }

    BizurException(String message) {
        super(message);
    }

    public BizurException(String message, Throwable cause) {
        super(message, cause);
    }
}
