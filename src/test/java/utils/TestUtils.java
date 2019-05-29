package utils;

import org.pmw.tinylog.Logger;

import java.util.Random;

public class TestUtils {

    private static final Random RANDOM;
    static {
        long seed = System.currentTimeMillis();
        Logger.error("Seed for random: " + seed);
        System.out.println("Seed for random: " + seed);
        System.err.println("Seed for random: " + seed);
        RANDOM = new Random(seed);
    }

    public static Random getRandom() {
        return RANDOM;
    }
}
