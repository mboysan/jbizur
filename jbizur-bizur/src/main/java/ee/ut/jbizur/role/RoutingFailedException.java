package ee.ut.jbizur.role;

public class RoutingFailedException extends Exception {
    public RoutingFailedException(Throwable cause) {
        super(cause);
    }

    public RoutingFailedException(String message) {
        super(message);
    }
}
