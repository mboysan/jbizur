package testframework;

import org.pmw.tinylog.Logger;
import testframework.result.IResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Used for collecting the test results.
 */
class ResultCollector {
    /**
     * Results collector executor service.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    /**
     * Latch map for each test group specified.
     */
    private final Map<String, CountDownLatch> testLatches = new ConcurrentHashMap<>();

    /**
     * Map that contains the results collected. Grouped by test group name
     */
    private final Map<String, List<IResult>> resultsMap = new ConcurrentHashMap<>();

    /**
     * Read-write lock for the collected results list.
     */
    private final ReadWriteLock resultsLock = new ReentrantReadWriteLock();

    public ResultCollector() {
    }

    /**
     * Adds result to the {@link #resultsMap} based on its properties. It adds the result in a synchronized manner.
     * @param result result to add
     */
    public void addResult(IResult result){
        String testGroupName = result.getTestGroupName();
        if(testGroupName == null || result.getTestPhase() == null){
            throw new IllegalArgumentException("testGroupName or testPhase should not be null");
        }

        resultsLock.writeLock().lock();
        try {
            List<IResult> results = resultsMap.get(testGroupName);
            if(results == null){
                results = new ArrayList<>();
            }
            results.add(result);
            resultsMap.put(testGroupName, results);
        } finally {
            resultsLock.writeLock().unlock();
        }
    }

    /**
     * Adds result to the {@link #resultsMap} based on its properties. It adds the result in a async manner.
     * @param result result to add
     */
    public void addResultAsync(IResult result){
        executor.execute(() -> {
            CountDownLatch latch = testLatches.get(result.getTestGroupName());
            if(latch == null){
                throw new IllegalArgumentException("Task count not specified!");
            }
            addResult(result);
            latch.countDown();
        });
    }

    /**
     * Adds result in an async manner. There is no bound in the collected result, hence it is the responsibility
     * of the caller to terminate the result collection process accordingly.
     * @param result result to add
     */
    public void addResultAsyncUnbounded(IResult result){
        executor.execute(() -> {
            addResult(result);
        });
    }

    /**
     * Prints the results collected to console.
     * @param testGroupName name of the test group to print.
     * @param phase         result phase to print. If null, then prints all the phases.
     */
    void printOnConsole(String testGroupName, TestPhase phase){
        if(phase == null){
            //print all, grouped by phase
            for (TestPhase testPhase : TestPhase.values()) {
                if(testGroupName == null){
                    for (String s : resultsMap.keySet()) {
                        printOnConsole(s, testPhase);
                    }
                } else {
                    printOnConsole(testGroupName, testPhase);
                }
            }
        } else {
            boolean printHeader = true;
            resultsLock.readLock().lock();
            try {
                List<IResult> results = resultsMap.get(testGroupName);
                for (IResult result : results) {
                    if(result.getTestPhase() == phase){
                        if(printHeader){
                            System.out.println("------------------------------ TEST GROUP: " + testGroupName);
                        }
                        System.out.print(result.CSVLine(printHeader));
                        printHeader = false;
                    }
                }
            } finally {
                resultsLock.readLock().unlock();
            }
        }
    }

    /**
     * Sets a latch with the specified count for a test group. Used with {@link #addResultAsync(IResult)} method that
     * will countdown the latch each time a result is added.
     *
     * @param testGroupName test group to set task count for
     * @param taskCount     count of the tasks to collect results for (should be equal to total results to collect
     *                      in an async manner)
     */
    void setTaskCountForTest(String testGroupName, int taskCount){
        if(testGroupName == null){
            throw new IllegalArgumentException("test group name should be specified");
        }
        if(taskCount < 0){
            throw new IllegalArgumentException("task count should be a positive integer value.");
        }
        testLatches.put(testGroupName, new CountDownLatch(taskCount));
    }

    /**
     * @param testGroupName test group to wait result collection for.
     */
    void waitAllTasksFor(String testGroupName){
        if(testGroupName == null){
            throw new IllegalArgumentException("test group name should be specified");
        }
        Logger.debug("Waiting tasks to complete for test [" + testGroupName +"]");
        CountDownLatch latch = testLatches.get(testGroupName);
        try {
            latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Logger.error(e, "Error while waiting for tasks to complete.");
        }
        Logger.debug("Wait done for test [" + testGroupName + "]");
    }

    /**
     * Finalizes the result collection by terminating the executor service.
     */
    void finalizeCollection(){
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Logger.error(e, "Could not terminate the executor");
        }
    }
}
