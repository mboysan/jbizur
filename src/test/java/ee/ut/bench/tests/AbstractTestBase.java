package ee.ut.bench.tests;

import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Random;

public abstract class AbstractTestBase {

    AbstractTest test;
    static Random random;

    @BeforeClass
    public static void initRandom() {
        long seed = System.currentTimeMillis() ^ 2;
        System.out.println("Seed for test: " + seed);
        random = new Random(seed);
    }

    @Before
    public abstract void setUp();

    abstract void testRunParallel();
}
