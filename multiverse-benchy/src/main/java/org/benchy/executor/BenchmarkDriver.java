package org.benchy.executor;

import org.benchy.TestCaseResult;

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

    void preRun(TestCase testCase);

    void run();

    void postRun(TestCaseResult caseResult);
}
