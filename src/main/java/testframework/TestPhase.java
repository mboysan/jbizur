package testframework;

/**
 * Indicates the test's phase.
 */
public enum TestPhase {
    /**
     * Warmup phase to ready JVM and JIT
     */
    PHASE_WARMUP("warmup", 50),
    /**
     * Full load phase
     */
    PHASE_FULL_LOAD("full-load", 100),
    /**
     * Custom phase
     */
    PHASE_CUSTOM("N/A", -1)
    ;

    /**
     * name of the phase
     */
    final String name;
    /**
     * preferred number of iterations for the active phase
     */
    final int iterations;
    TestPhase(String name, int iterations) {
        this.name = name;
        this.iterations = iterations;
    }

    /**
     * @return gets {@link #name}
     */
    public String getName(){
        return name;
    }

    /**
     * @return gets {@link #iterations}
     */
    public int getIterations(){
        return iterations;
    }
}
