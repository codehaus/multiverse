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

    private volatile boolean stop;
    private int readThreadCount = 10;
    private int refCount = 100;

    private final AtomicLong inconsistenciesCounter = new AtomicLong();
    private Ref[] refs = new Ref[refCount];


    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        inconsistenciesCounter.set(0);
        stop = false;
    }

    @Test
    public void test() {
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new Ref();
        }

        ReadThread[] readThreads = new ReadThread[readThreadCount];
        for (int k = 0; k < readThreads.length; k++) {
            readThreads[k] = new ReadThread(k);
        }

        ModifyThread modifyThread = new ModifyThread(0);

        startAll(readThreads);
        startAll(modifyThread);
        sleepMs(getStressTestDurationMs(60 * 1000));

        stop = true;
        joinAll(readThreads);
        joinAll(modifyThread);
        System.out.println("number of readinconsistencies: " + inconsistenciesCounter.get());
        assertTrue(inconsistenciesCounter.get() > 0);
    }

    class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                for (Ref ref : refs) {
                    if (randomOneOf(10)) {
                        sleepRandomUs(10);
                        ref.inc();
                    }
                }
            }
        }
    }

    class ReadThread extends TestThread {

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int readInconsistencies = 0;

            int k = 0;
            while (!stop) {

                readInconsistencies += doread();

                if (k % 10000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                k++;
            }

            inconsistenciesCounter.addAndGet(readInconsistencies);
        }

        //important that this method is not transactional.

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
