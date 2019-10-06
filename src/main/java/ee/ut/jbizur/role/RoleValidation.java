package ee.ut.jbizur.role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleValidation {

    private static final Logger logger = LoggerFactory.getLogger(RoleValidation.class);

    public static boolean checkStateAndWarn(boolean isOk, String message) {
        if (isOk) {
            return true;
        }
        logger.warn(message);
        return false;
    }

    public static void checkStateAndError(boolean isOk, String message) throws IllegalStateException {
        if (isOk) {
            return;
        }
        logger.error(message);
        throw new IllegalStateException(message);
    }
}
