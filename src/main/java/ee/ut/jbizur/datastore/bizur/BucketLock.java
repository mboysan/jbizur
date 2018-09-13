package ee.ut.jbizur.datastore.bizur;

import org.pmw.tinylog.Logger;

public class BucketLock {
    private boolean isLocked = false;

    public synchronized void lock() throws InterruptedException{
        while(isLocked){
            long start = System.currentTimeMillis();
            wait(3000);
            long totalTime = System.currentTimeMillis() - start;
            if (totalTime >= 3000) {
                Logger.warn("waited for too long");
            }
        }
        isLocked = true;
    }

    public synchronized void unlock(){
        isLocked = false;
        notify();
    }
}
