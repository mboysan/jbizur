package ee.ut.jbizur.role;

import org.pmw.tinylog.Logger;

public class RoleValidation {

    public static boolean checkStateAndWarn(boolean isOk, String message) {
        if (isOk) {
            return true;
        }
        Logger.warn(message);
        return false;
    }

    public static void checkStateAndError(boolean isOk, String message) {
        if (isOk) {
            return;
        }
        Logger.error(message);
        throw new IllegalStateException(message);
    }
}
