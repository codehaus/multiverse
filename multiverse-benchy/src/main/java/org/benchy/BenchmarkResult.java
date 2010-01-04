package org.benchy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Peter Veentjer
 */
public class BenchmarkResult {

    private final String benchmarkName;
    private final List<TestCaseResult> testCaseResultList;

    public BenchmarkResult(String benchmarkName) {
        this(benchmarkName, new LinkedList<TestCaseResult>());
    }

    public BenchmarkResult(String benchmarkName, List<TestCaseResult> testCaseResultList) {
        if (testCaseResultList == null) {
            throw new NullPointerException();
        }

        this.testCaseResultList = testCaseResultList;
        this.benchmarkName = benchmarkName;
    }

    public String getBenchmarkName() {
        return benchmarkName;
    }

    public void add(TestCaseResult result) {
        if (result == null) {
            throw new NullPointerException();
        }
        testCaseResultList.add(result);
    }

    public List<TestCaseResult> getTestCaseResultList() {
        return Collections.unmodifiableList(testCaseResultList);
    }
}
