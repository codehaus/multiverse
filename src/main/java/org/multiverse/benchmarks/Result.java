package org.multiverse.benchmarks;

/**
 * @author Peter Veentjer
 */
public class Result {
    public int processorCount;
    public double performance;

    public Result(int processorCount, double performance) {
        this.processorCount = processorCount;
        this.performance = performance;
    }
}
