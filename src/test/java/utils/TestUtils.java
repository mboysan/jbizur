package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

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
