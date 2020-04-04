package ee.ut.jbizur.common;

import java.util.function.Predicate;

public class CountdownPredicate<T> extends CountdownLambda implements Predicate<T> {

    public CountdownPredicate(int count, long timeoutMillis) {
        super(count, timeoutMillis);
    }

    public CountdownPredicate(int count) {
        super(count);
    }

    @Override
    public boolean test(T t) {
        countdown();
        return true;
    }
}
