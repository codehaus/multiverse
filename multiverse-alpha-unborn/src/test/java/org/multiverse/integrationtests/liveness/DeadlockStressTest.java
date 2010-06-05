package org.multiverse.integrationtests.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;


/**
 * A test that checks that committing transactions, won't cause deadlocks.
 * <p/>
 * Tests for direct deadlocks and deadlock chains.
 */
public class DeadlockStressTest {

    private volatile boolean stop;
    private int refCount = 100;
    private int threadCount = 10;
    private IntRef[] refs;
    private ChangeThread[] threads;

    @Before
    public void setUp() {
        stop = false;
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        refs = new IntRef[refCount];
        for (int k = 0; k < refCount; k++) {
            refs[k] = new IntRef();
        }

        threads = new ChangeThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new ChangeThread(k);
        }

        startAll(threads);
        sleepMs(getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);
    }

    public class ChangeThread extends TestThread {

        public ChangeThread(int id) {
            super("ChangeThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            int k = 0;
            while (!stop) {
                if (k % 100000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                transaction();
                k++;
            }
        }

        @TransactionalMethod
        public void transaction() {
            for (int k = 0; k < refs.length; k++) {
                if (randomInt(3) == 0) {
                    int index = randomInt(refs.length);

                    if (randomInt(5) == 0) {
                        refs[index].inc();
                    }
                }
            }
        }
    }
}
