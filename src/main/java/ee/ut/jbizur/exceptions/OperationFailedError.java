package ee.ut.jbizur.exceptions;

public class OperationFailedError extends Error {
    public OperationFailedError(String message) {
        super(message);
    }
}
