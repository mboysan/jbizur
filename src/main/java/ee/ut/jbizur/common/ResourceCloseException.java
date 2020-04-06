package ee.ut.jbizur.common;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ResourceCloseException extends RuntimeException {

    private final Collection<Exception> exceptions;

    public ResourceCloseException(String message, Exception exception) {
        this(message, List.of(exception));
    }

    public ResourceCloseException(String allMessages, Collection<Exception> exceptions) {
        super(allMessages);
        Objects.requireNonNull(exceptions);
        this.exceptions = exceptions;
    }

    @Override
    public void printStackTrace() {
        for (Exception exception : exceptions) {
            exception.printStackTrace();
        }
    }
}
