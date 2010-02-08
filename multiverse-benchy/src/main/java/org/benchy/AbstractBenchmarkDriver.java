package org.benchy;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCase;
import org.benchy.TestCaseResult;

/**
 * A convenience {@link BenchmarkDriver} implementation that doesn't force one to implement optional
 * methods. Only the {@link BenchmarkDriver#run()} method needs to be implemented.
 *
 * @author Peter Veentjer.
 */
public abstract class AbstractBenchmarkDriver implements BenchmarkDriver {

    @Override
    public void preRun(TestCase testCase) {
    }

    @Override
    public void postRun(TestCaseResult caseResult) {
    }
}
