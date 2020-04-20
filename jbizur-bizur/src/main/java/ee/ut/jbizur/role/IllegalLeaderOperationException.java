package ee.ut.jbizur.role;

import java.io.Serializable;

public class IllegalLeaderOperationException extends Exception implements Serializable {
    public IllegalLeaderOperationException(String who, int index) {
        this(who + " is not the leader of bucket=" + index);
    }

    public IllegalLeaderOperationException(String message) {
        super(message);
    }
}
