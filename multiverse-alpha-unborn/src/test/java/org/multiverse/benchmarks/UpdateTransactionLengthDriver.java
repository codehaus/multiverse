package org.multiverse.benchmarks;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.TestCase;
import org.benchy.TestCaseResult;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import java.util.concurrent.TimeUnit;

/**
 * A Driver that tests how the stm behaves with different transaction length
 * with 100% updates.
 *
 * @author Peter Veentjer
 */
public class UpdateTransactionLengthDriver extends AbstractBenchmarkDriver {

    private int transactionCount;
    private int transactionLength;

    private IntRef[] refs;

    @Override
    public void preRun(TestCase testCase) {
        transactionCount = testCase.getIntProperty("transactionCount");
        transactionLength = testCase.getIntProperty("transactionLength");

        refs = new IntRef[transactionLength];
        for (int k = 0; k < transactionLength; k++) {
            refs[k] = new IntRef();
        }
    }

    @Override
    public void run() {
        for (int k = 0; k < transactionCount; k++) {
            incAll();
        }
    }

    @TransactionalMethod
    public void incAll() {
        for (int k = 0; k < refs.length; k++) {
            refs[k].inc();
        }
    }

    @Override
    public void postRun(TestCaseResult caseResult) {
        caseResult.put("transactionCount", transactionCount);

        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1))
                / caseResult.getLongProperty("duration(ns)");
        caseResult.put("transactions/s", transactionsPerSecond);
    }
}
