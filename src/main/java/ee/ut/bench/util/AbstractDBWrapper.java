package ee.ut.bench.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class AbstractDBWrapper {

    private Random random;
    protected List<DBOperation> availableRandomOperations;

    public AbstractDBWrapper() {
        initRandom();
        this.availableRandomOperations = Arrays
                .stream(DBOperation.values())
                .filter(operation -> !operation.equals(DBOperation.RANDOM))
                .collect(Collectors.toList());
    }

    protected void initRandom() {
        long seed = System.currentTimeMillis();
        System.out.println("seed for [" + toString() + "]: " + seed);
        this.random = new Random(seed);
    }

    abstract void init(String... args) throws Exception;
    public abstract void reset();

    public <T> T run(DBOperation operation, String... args) {
        switch (operation) {
            case SET:
                return set(args[0], args[1]);
            case GET:
                return get(args[0]);
            case DELETE:
                return delete(args[0]);
            case ITERATE_KEYS:
                return (T) iterateKeys();
            case DEFAULT:
                return run(DBOperation.SET, args);
            case RANDOM: {
                return run(availableRandomOperations.get(random.nextInt(availableRandomOperations.size())), args);
            }
        }
        throw new IllegalArgumentException("operation is not supported: " + operation);
    }

    public abstract <T> T set(String key, String value);
    public abstract <T> T get(String key);
    public abstract <T> T delete(String key);
    public abstract Collection<String> iterateKeys();
}
