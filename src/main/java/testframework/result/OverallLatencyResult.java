package testframework.result;

import testframework.TestPhase;

/**
 * A collection of latencies.
 */
public class OverallLatencyResult implements IResult {
    /**
     * test group's name
     */
    private final String testGroupName;
    /**
     * test phase to record the result for
     */
    private final TestPhase testPhase;

    /**
     * generated internally when the object is created.
     */
    private final long timestamp;

    /**
     * number of processes in the system
     */
    private final int numberOfProcesses;
    /**
     * collected latency results.
     */
    private final long[] latencies;

    /**
     * average latency calculated with the {@link #latencies}
     */
    private final long averageLatency;
    /**
     * equals to {@link #latencies}.length
     */
    private final int resultsTotal;

    /**
     * @param testGroupName     test group's name
     * @param testPhase         test phase to record the result for
     * @param numberOfProcesses number of processes in the system
     * @param latencies         collected latency results.
     */
    public OverallLatencyResult(String testGroupName, TestPhase testPhase, int numberOfProcesses, long[] latencies) {
        this.testGroupName = testGroupName;
        this.testPhase = testPhase;
        this.numberOfProcesses = numberOfProcesses;
        this.latencies = latencies;

        this.timestamp = System.currentTimeMillis();

        this.averageLatency = calcAverage(latencies);
        this.resultsTotal = latencies.length;
    }

    /**
     * Calculates average latency with the given results.
     * @param results collection of latency results.
     * @return average latency from the collected results.
     */
    private static long calcAverage(long[] results){
        long avg = 0;
        for (long result : results) {
            avg += result;
        }
        return avg / results.length;
    }

    @Override
    public String getTestGroupName() {
        return testGroupName;
    }

    @Override
    public TestPhase getTestPhase() {
        return testPhase;
    }

    @Override
    public String CSVLine(boolean writeHeader) {
        String line = "";
        if(writeHeader){
            line += String.format("testGroup,phase,timestamp,time,iterations,resultsTotal,numProcs,avgLatency%n");
        }
        String time = timeStampReadable(timestamp);
        return line + String.format("%s,%s,%d,%s,%d,%d,%s,%d%n",
                testGroupName, testPhase.getName(), timestamp, time,
                testPhase.getIterations(), resultsTotal, numberOfProcesses, averageLatency);
    }
}
