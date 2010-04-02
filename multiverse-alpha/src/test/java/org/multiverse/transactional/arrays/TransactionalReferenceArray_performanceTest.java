package org.multiverse.transactional.arrays;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * The performance difference atm is that the transactionalreferencearray is almost
 * 10 times slower than the  AtomicReferenceArray.
 *
 * @author Peter Veentjer.
 */
public class TransactionalReferenceArray_performanceTest {


    public final long transactionCount = 1000 * 1000 * 1000;
    public final int itemCount = 1000;

    @Test
    public void testTransactionalReferenceArray() {
        TransactionalReferenceArray<String> list = new TransactionalReferenceArray<String>(itemCount);

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            String s = (k % 2) == 0 ? "foo" : "bar";

            list.set((int) k % itemCount, s);

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }

    @Test
    public void testAtomicReferenceArray() {
        AtomicReferenceArray<String> list = new AtomicReferenceArray<String>(itemCount);

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            String s = (k % 2) == 0 ? "foo" : "bar";

            list.set((int) k % itemCount, s);

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", transactionsPerSecond);
    }

}
