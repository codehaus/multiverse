package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaRef_setPerformanceTest {

    private long transactionCount = ((long) 1000) * 1000 * 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void set() {
        AlphaRef<String> ref = new AlphaRef<String>();

        long startNs = System.nanoTime();


        for (long k = 0; k < transactionCount; k++) {
            ref.set(k % 2 == 0 ? "foo" : "bar");

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }

    @Test
    public void setAtomic() {
        AlphaRef<String> ref = new AlphaRef<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.setAtomic(k % 2 == 0 ? "foo" : "bar");

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }
}
