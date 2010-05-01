package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.format;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * This performance test indicates that the most time is spend on the openForRead method
 * (so also inside methods being called from open for read since they could have been inlined).
 * <p/>
 * With smart transaction inlining there could 10/20 fold performance improvement (the
 * alpharef already is able to do very cheap readonly transactions).
 *
 * @author Peter Veentjer
 */
public class TransactionalArrayList_getPerformanceTest {

    public final long transactionCount = 100 * 1000 * 1000;
    public final int itemCount = 1000;
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        for (int k = 0; k < 1000; k++) {
            list.add("" + k);
        }

        long version = stm.getVersion();

        long startNs = System.nanoTime();

        for (long k = 0; k < transactionCount; k++) {
            list.get((int) (k % itemCount));

            if (k % (10 * 1000 * 1000) == 0) {
                System.out.println("at: " + k);
            }
        }

        //version should not have increaded in this test.
        assertEquals(version, stm.getVersion());

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));
    }
}
