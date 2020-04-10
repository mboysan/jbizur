package ee.ut.jbizur.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public final class RngUtil {

    private static final Logger logger = LoggerFactory.getLogger(RngUtil.class);

    private RngUtil() {}

    private static final long seed = System.currentTimeMillis();
    private static final Random RANDOM = new Random(seed);
    static {
        logger.warn("seed: {}", seed);
    }

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }
}
