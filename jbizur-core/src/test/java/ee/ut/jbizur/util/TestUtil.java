package ee.ut.jbizur.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class TestUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

    private static final Random RANDOM;
    static {
        long seed = System.currentTimeMillis();
        logger.error("Seed for random: {}", seed);
        System.out.println("Seed for random: " + seed);
        RANDOM = new Random(seed);
    }

    public static Random getRandom() {
        return RANDOM;
    }
}
