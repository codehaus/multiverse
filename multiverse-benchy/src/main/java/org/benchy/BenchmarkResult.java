package org.benchy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the results of a Benchmark.
 *
 * @author Peter Veentjer
 */
public class BenchmarkResult {

    private final String benchmarkName;
    private final List<TestCaseResult> testCaseResultList;

    public BenchmarkResult(String benchmarkName) {
        this(benchmarkName, new LinkedList<TestCaseResult>());
    }

    public BenchmarkResult(String benchmarkName, List<TestCaseResult> testCaseResultList) {
        if (benchmarkName == null) {
            throw new NullPointerException();
        }

        if (testCaseResultList == null) {
            throw new NullPointerException();
        }

        this.testCaseResultList = testCaseResultList;
        this.benchmarkName = benchmarkName;
    }

    /**
     * Returns the name of the Benchmark.
     *
     * @return the result of the benchmark.
     */
    public String getBenchmarkName() {
        return benchmarkName;
    }

    /**
     * Adds a TestCaseResult to this BenchmarkResult.
     *
     * @param result the TestCaseResult to add.
     * @throws NullPointerException if result is null.
     */
    public void add(TestCaseResult result) {
        if (result == null) {
            throw new NullPointerException();
        }
        testCaseResultList.add(result);
    }

    public List<TestCaseResult> getTestCaseResults() {
        return Collections.unmodifiableList(testCaseResultList);
    }
}
