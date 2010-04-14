package org.multiverse.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ConcurrentUpdateStressTest {

    public TransactionalInteger ref;
    public int incCount = 10 * 1000 * 1000;
    public int threadCount = 3;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        ref = new TransactionalInteger(0);
    }

    @After
    public void tearDown() {
        //    stm.getProfiler().print();
    }

    @Test
    public void test() {
        UpdateThread[] threads = createThreads();

        long startNs = System.nanoTime();

        startAll(threads);
        joinAll(threads);

        assertEquals(threadCount * incCount, ref.get());

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (incCount * threadCount * 1.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);
    }

    public UpdateThread[] createThreads() {
        UpdateThread[] results = new UpdateThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            results[k] = new UpdateThread(k);
        }
        return results;
    }

    public class UpdateThread extends TestThread {

        public UpdateThread(int id) {
            super("UpdateThread-" + id);
        }

        @Test
        public void doRun() {
            for (int k = 0; k < incCount; k++) {
                ref.inc();

                if (k % (1000 * 1000) == 0) {
                    System.out.printf("%s at %s\n", getName(), k);
                }
            }
        }
    }
}

