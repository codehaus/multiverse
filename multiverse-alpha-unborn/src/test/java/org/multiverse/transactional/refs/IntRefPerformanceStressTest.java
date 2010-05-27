package org.multiverse.transactional.refs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.format;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class IntRefPerformanceStressTest {

    private int count = 50;// * 1000 * 1000;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        IntRef ref = new IntRef();

        long startNs = System.nanoTime();

        for (int k = 0; k < count; k++) {
            ref.inc();

            if (k % 5000000 == 0) {
                System.out.printf("at %s\n", k);
            }
        }

        assertEquals(count, ref.get());
        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 1.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", format(transactionPerSecond));
    }
}
