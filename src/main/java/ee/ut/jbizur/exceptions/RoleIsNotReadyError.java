package ee.ut.jbizur.exceptions;

public class RoleIsNotReadyError extends Error {
    public RoleIsNotReadyError(String message) {
        super(message);
    }

    public RoleIsNotReadyError(Throwable cause) {
        super(cause);
    }
}
