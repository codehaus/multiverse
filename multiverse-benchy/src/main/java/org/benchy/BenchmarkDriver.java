package org.benchy;

/**
 * Contains the unparametrized algorithm you want to benchmark. All the variable parameters
 * are stored in the {@link TestCase}. It could be that the same driver is used in different
 * to benchmark different aspects of the algorithm.
 * <p/>
 * It looks a lot like a basic unit testcase with the familiar lifecyle methods. For a more
 * convenient implementation to extend from, look at the {@link AbstractBenchmarkDriver}.
 *
 * @author Peter Veentjer.
 */
public interface BenchmarkDriver {

    /**
     * Is executed before the run.
     *
     * @param testCase the testCase that is going to be run.
     */
    void preRun(TestCase testCase);

    /**
     * Runs the driver for one iteration.
     */
    void run();

    /**
     * Is executed after the run.
     *
     * @param caseResult the testCase that just has run.
     */
    void postRun(TestCaseResult caseResult);
}
