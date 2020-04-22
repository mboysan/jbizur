package ee.ut.jbizur.role;

public class RoutingFailedException extends BizurException {
    public RoutingFailedException(Throwable cause) {
        super(cause);
    }

    public RoutingFailedException(String message) {
        super(message);
    }

    public RoutingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
