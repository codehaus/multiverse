package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.format;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalArrayList_setPerformanceTest {

    public final long transactionCount = 1000 * 1000 * 10;
    public final int itemCount = 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        for (int k = 0; k < itemCount; k++) {
            list.add("" + k);
        }

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
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }
}
