package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A performance comparison between the TransactionalReferenceArray.set and the
 * AtomicReferenceArray.set.
 * <p/>
 * On my machine the first one does around the 12M transactions/second and the latter
 * one does 64M transactions/second.
 * <p/>
 * One few of the reasons that the TransactionalReferenceArray is slower is
 * that more cas operations are needed (also the version and lock) and that the
 * transaction needs to be checked from the TransactionThreadLocal.
 *
 * @author Peter Veentjer.
 */
public class TransactionalReferenceArray_setPerformanceTest {


    public final long transactionCount = 300 * 1000 * 1000;
    public final int itemCount = 100000;
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testTransactionalReferenceArray() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(itemCount);

        long version = stm.getVersion();
        long startNs = System.nanoTime();

        String value = "";

        for (long k = 0; k < transactionCount; k++) {

            array.set((int) k % itemCount, value);

            if (k % (50 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }

            if (k % itemCount == 0) {
                value += " ";
            }
        }

        long durationNs = System.nanoTime() - startNs;
        assertEquals(version + transactionCount, stm.getVersion());
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void testTransactionalReferenceArrayUsingAtomicSet() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(itemCount);

        long version = stm.getVersion();
        long startNs = System.nanoTime();

        String value = "";

        for (long k = 0; k < transactionCount; k++) {

            array.atomicSet((int) k % itemCount, value);

            if (k % (50 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }

            if (k % itemCount == 0) {
                value += " ";
            }
        }

        long durationNs = System.nanoTime() - startNs;
        assertEquals(version + transactionCount, stm.getVersion());
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }

    @Test
    public void testAtomicReferenceArray() {
        AtomicReferenceArray<String> array = new AtomicReferenceArray<String>(itemCount);

        long startNs = System.nanoTime();

        String value = " ";

        for (long k = 0; k < transactionCount; k++) {
            array.set((int) k % itemCount, value);

            if (k % (50 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }

            if (k % itemCount == 0) {
                value += " ";
            }
        }

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }
}
