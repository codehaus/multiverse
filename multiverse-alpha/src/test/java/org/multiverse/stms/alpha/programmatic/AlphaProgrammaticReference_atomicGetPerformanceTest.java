package org.multiverse.stms.alpha.programmatic;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReference_atomicGetPerformanceTest {

    private long transactionCount = ((long) 1000) * 1000 * 1000 * 2;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void getAtomic() {
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.atomicGet();

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }

    @Test
    public void get() {
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.get();

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }
}
