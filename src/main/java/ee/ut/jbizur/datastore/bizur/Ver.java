package ee.ut.jbizur.datastore.bizur;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Ver {
    private AtomicInteger electId;
    private AtomicInteger votedElectId;
    private AtomicInteger counter;

    public Ver() {
//        int initId = new Random().nextInt(100);
        int initId = 0;
        electId = new AtomicInteger(initId);
        votedElectId = new AtomicInteger(initId);
        counter = new AtomicInteger(0);
    }

    public int getElectId() {
        return electId.get();
    }

    public Ver setElectId(int electId) {
        this.electId.set(electId);
        return this;
    }

    public int incrementAndGetElectId() {
        return electId.incrementAndGet();
    }

    public Ver setVotedElectedId(int votedElectedId) {
        this.votedElectId.set(votedElectedId);
        return this;
    }

    public int getVotedElectId() {
        return votedElectId.get();
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

    public int compareTo(Ver other){
        if(getElectId() > other.getElectId()){
            return 1;
        } else if (getElectId() == other.getElectId()){
            return Integer.compare(getCounter(), other.getCounter());
        } else {
            return -1;
        }
    }

    public static int compare(Ver v1, Ver v2){
        return v1.compareTo(v2);
    }
}
