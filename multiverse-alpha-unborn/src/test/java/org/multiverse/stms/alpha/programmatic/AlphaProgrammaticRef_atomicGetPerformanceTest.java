package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.format;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Performance comparison between AlphaProgrammaticRef.atomicGet and the
 * AtomicReference.get
 * <p/>
 * On my machine on a single core both are around the 75M transactions/second.
 * The reason why the performance is the same, is the both only need a volatile
 * read and all transaction overhead has been removed from the atomicGet
 * (transaction is completely inlined).
 *
 * @author Peter Veentjer
 */
public class AlphaProgrammaticRef_atomicGetPerformanceTest {

    private long transactionCount = ((long) 1000) * 1000 * 1000 * 2;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void getAtomic() {
        AlphaProgrammaticRef<String> ref = new AlphaProgrammaticRef<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.atomicGet();

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void get() {
        AlphaProgrammaticRef<String> ref = new AlphaProgrammaticRef<String>();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            ref.get();

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }
}
