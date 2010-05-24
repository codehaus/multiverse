package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.multiverse.TestUtils.format;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A performance comparison of the get operation between the
 * {@link TransactionalReferenceArray} and the {@link java.util.concurrent.atomic.AtomicReferenceArray}.
 * <p/>
 * On my machine I get around 80M transaction/second for the Ref and
 * 180M transactions/second for the AtomicReferenceArray.
 *
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_getPerformanceTest {

    public final long transactionCount = 2000 * 1000 * 1000;
    public final int itemCount = 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testTransactionalReferenceArray() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(itemCount);

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            array.get((int) k % itemCount);

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void testTransactionalReferenceArrayUsingAtomicGet() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(itemCount);

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            array.atomicGet((int) k % itemCount);

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void testAtomicReferenceArray() {
        AtomicReferenceArray<String> array = new AtomicReferenceArray<String>(itemCount);

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            array.get((int) k % itemCount);

            if (k % (100 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }
}
