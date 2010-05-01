package org.multiverse.integrationtests.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.integrationtests.Ref;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A test that checks that if multiple transactions are used, you get read inconsistencies.
 * Normally you want to have a perfect consistent view over the world, but this is at some cost.
 *
 * @author Peter Veentjer
 */
public class NoUnwantedReadConsistencyStressTest {

    public long readCount = 500 * 1000;
    public int readThreadCount = 10;
    public int refCount = 100;

    public volatile boolean finished = false;

    public final AtomicLong inconsistenciesCounter = new AtomicLong();

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        inconsistenciesCounter.set(0);
    }

    @Test
    public void test() {
        Ref[] refs = new Ref[refCount];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new Ref();
        }

        ReadThread[] threads = new ReadThread[readThreadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(refs, k);
        }

        startAll(threads);

        while (!finished) {
            for (Ref ref : refs) {
                if (randomOneOf(10)) {
                    sleepRandomUs(10);
                    ref.inc();
                }
            }
        }

        joinAll(threads);
        System.out.println("number of readinconsistencies: " + inconsistenciesCounter.get());
        assertTrue(inconsistenciesCounter.get() > 0);
    }

    class ReadThread extends TestThread {
        private Ref[] refs;


        public ReadThread(Ref[] refs, int id) {
            super("ReadThread-" + id);
            this.refs = refs;
        }

        @Override
        public void doRun() throws Exception {
            int readInconsistencies = 0;

            for (int k = 0; k < readCount; k++) {
                readInconsistencies += doread();

                if (k % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            finished = true;
            inconsistenciesCounter.addAndGet(readInconsistencies);
        }

        private int doread() {
            int[] values = new int[refs.length];
            for (int k = 0; k < refs.length; k++) {
                values[k] = refs[k].get();
            }

            sleepRandomUs(10);

            int inconsistencies = 0;
            for (int k = 0; k < refs.length; k++) {
                if (values[k] != refs[k].get()) {
                    inconsistencies++;
                }
            }
            return inconsistencies;
        }
    }
}
