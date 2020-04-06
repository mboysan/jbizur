package ee.ut.jbizur.common;

import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public interface ResourceCloser {

    default void closeResources(Logger logger, AutoCloseable... closeables) {
        closeResources(logger, Arrays.stream(closeables).collect(Collectors.toSet()));
    }

    default void closeResources(Logger logger, Collection<? extends AutoCloseable> closeables) {
        try {
            closeResources(closeables);
        } catch (ResourceCloseException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    default void closeResources(Logger logger, Stack<AutoCloseable> stack) {
        while (!stack.isEmpty()) {
            closeResources(logger, stack.pop());
        }
    }

    default void closeResources(Stack<AutoCloseable> stack) {
        while (!stack.isEmpty()) {
            closeResources(stack.pop());
        }
    }

    default void closeResources(AutoCloseable... closeables) throws ResourceCloseException {
        closeResources(Arrays.stream(closeables).collect(Collectors.toSet()));
    }

    default void closeResources(Collection<? extends AutoCloseable> closeables) throws ResourceCloseException {
        if (closeables != null) {
            List<Exception> exceptions = new ArrayList<>();
            StringJoiner sj = new StringJoiner(String.format("%n"));
            for (AutoCloseable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        String message = String.format("Resource could not be closed=[%s], reason=[%s]",
                                closeable.toString(), e.getMessage());
                        sj.add(message);
                        exceptions.add(e);
                    }
                }
            }
            if (exceptions.size() > 0) {
                throw new ResourceCloseException(sj.toString(), exceptions);
            }
        }
    }

}
