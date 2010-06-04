package org.multiverse.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.transactional.refs.IntRef;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ConcurrentUpdateStressTest {

    private IntRef ref;
    private volatile boolean stop;
    private int threadCount = 3;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        ref = new IntRef(0);
        stop = false;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        UpdateThread[] threads = createThreads();

        long startNs = System.nanoTime();

        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        long count = sum(threads);

        assertEquals(count, ref.get());

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond = (count * 1.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", format(transactionPerSecond));
    }

    public UpdateThread[] createThreads() {
        UpdateThread[] results = new UpdateThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            results[k] = new UpdateThread(k);
        }
        return results;
    }

    private long sum(UpdateThread[] threads){
        long result = 0;
        for(UpdateThread t: threads){
            result+=t.count;
        }
        return result;
    }

    public class UpdateThread extends TestThread {

        private long count;

        public UpdateThread(int id) {
            super("UpdateThread-" + id);
        }

        @Override
        public void doRun() {
            while (!stop) {
                ref.inc();

                if (count % (1000 * 1000) == 0) {
                    System.out.printf("%s at %s\n", getName(), count);
                }

                count++;
            }
        }
    }
}

