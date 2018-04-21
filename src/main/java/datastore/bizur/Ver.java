package datastore.bizur;

import java.util.concurrent.atomic.AtomicInteger;

public class Ver {
    private AtomicInteger electId = new AtomicInteger(0);
    private AtomicInteger counter = new AtomicInteger(0);

    public int getElectId() {
        return electId.get();
    }

    public Ver setElectId(int electId) {
        this.electId.set(electId);
        return this;
    }

    public int getCounter() {
        return counter.get();
    }

    public Ver setCounter(int counter) {
        this.counter.set(counter);
        return this;
    }

    public Ver incrementCounter(){
        this.counter.getAndIncrement();
        return this;
    }
}
