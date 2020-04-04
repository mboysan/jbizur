package ee.ut.jbizur.common;

import java.util.function.Consumer;

public class CountdownConsumer<T> extends CountdownLambda implements Consumer<T> {
    public CountdownConsumer(int count, long timeoutMillis) {
        super(count, timeoutMillis);
    }

    public CountdownConsumer(int count) {
        super(count);
    }

    @Override
    public void accept(T t) {
        countdown();
    }


}
