package ee.ut.jbizur.role;

public class OperationFailedException extends BizurException {
    public OperationFailedException(String who, int index) {
        super(who, "operation failed", index);
    }
    public OperationFailedException(String message) {
        super(message);
    }
}
