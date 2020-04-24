package ee.ut.jbizur.role;

public class IllegalLeaderOperationException extends BizurException {
    public IllegalLeaderOperationException(String who, int index) {
        super(who, "is not the leader", index);
    }
}
