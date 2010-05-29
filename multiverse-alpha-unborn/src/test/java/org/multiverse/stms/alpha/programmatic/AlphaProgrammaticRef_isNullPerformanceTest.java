package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRef_isNullPerformanceTest {

    private long transactionCount = ((long) 1000) * 1000 * 100;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void isNullAtomic() {
        AlphaProgrammaticRef<String> ref = new AlphaProgrammaticRef<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.atomicIsNull();

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }

    @Test
    public void isNull() {
        AlphaProgrammaticRef<String> ref = new AlphaProgrammaticRef<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.isNull();

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }
}
