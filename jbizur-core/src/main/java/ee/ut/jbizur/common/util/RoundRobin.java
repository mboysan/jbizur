package ee.ut.jbizur.common.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class RoundRobin<T> implements Iterable<T> {
    private final List<T> coll;

    public RoundRobin(List<T> coll) {
        this.coll = coll;
    }

    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public synchronized T next() throws NoSuchElementException {
                if (!hasNext()) {   // by contract
                    throw new NoSuchElementException();
                }
                T res = coll.get(index);
                index = (index + 1) % coll.size();
                return res;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
