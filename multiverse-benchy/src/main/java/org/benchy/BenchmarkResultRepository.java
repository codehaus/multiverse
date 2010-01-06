package org.benchy;

import java.util.Date;

/**
 * The TestResultRepository is responsible for storing and retrieving {@link TestCaseResult}.
 *
 * @author Peter Veentjer
 */
public interface BenchmarkResultRepository {

    /**
     * Loads all the testresults for a specific benchmark executed on a specific date.
     *
     * @param date          the data the benchmark was executed
     * @param benchmarkName the name of the benchmark.
     * @return a list containing all TestResults for that specific benchmark on that specific
     *         date. If no items are found, an empty list is returned.
     * @throws NullPointerException if date or benchmarkname is null.
     */
    BenchmarkResult load(Date date, String benchmarkName);

    BenchmarkResult loadLast(Date date, String benchmarkName);

    BenchmarkResult loadLast(String benchmark);

    /**
     * Stores the TestResult in the repository.
     *
     * @param benchmarkResult the TestResult to store.
     * @throws NullPointerException if testResult is null.
     */
    void store(BenchmarkResult benchmarkResult);
}
