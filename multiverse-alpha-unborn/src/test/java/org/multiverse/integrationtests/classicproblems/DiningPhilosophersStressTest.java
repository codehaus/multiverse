package org.multiverse.integrationtests.classicproblems;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * The cause of the dining philosophers problem is that the take of the left and right fork are not atomic. So it
 * could happen that all philosophers have their left fork, but won't get the right for because the philosopher sitting
 * right to them has that fork.
 * <p/>
 * Within the MultiversionedStm both forks are acquired atomically (so a philosopher gets them both, or won't
 * get them at all).
 *
 * @author Peter Veentjer.
 */
public class DiningPhilosophersStressTest {
    private int philosopherCount = 10;
    private volatile boolean stop;

    private IntRef[] forks;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        createForks();

        PhilosopherThread[] philosopherThreads = createPhilosopherThreads();
        startAll(philosopherThreads);

        sleepMs(getStressTestDurationMs(60*1000));
        
        stop = true;
        joinAll(philosopherThreads);

        assertAllForksHaveReturned();
    }

    public void assertAllForksHaveReturned() {
        for (IntRef fork : forks) {
            assertEquals(0, fork.get());
        }
    }

    public PhilosopherThread[] createPhilosopherThreads() {
        PhilosopherThread[] threads = new PhilosopherThread[philosopherCount];
        for (int k = 0; k < philosopherCount; k++) {
            IntRef leftFork = forks[k];
            IntRef rightFork = k == philosopherCount - 1 ? forks[0] : forks[k + 1];
            threads[k] = new PhilosopherThread(k, leftFork, rightFork);
        }
        return threads;
    }

    public void createForks() {
        forks = new IntRef[philosopherCount];
        for (int k = 0; k < forks.length; k++) {
            forks[k] = new IntRef(0);
        }
    }

    class PhilosopherThread extends TestThread {
        private final IntRef leftFork;
        private final IntRef rightFork;

        PhilosopherThread(int id, IntRef leftFork, IntRef rightFork) {
            super("PhilosopherThread-" + id);
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        @Override
        public void doRun() {
            int k = 0;
            while (!stop) {
                if (k % 100 == 0) {
                    System.out.printf("%s at %s\n", getName(), k);
                }
                eat();
                k++;
            }
        }

        public void eat() {
            takeForks();
            stuffHole();
            releaseForks();
        }

        private void stuffHole() {
            //simulate the eating
            sleepRandomMs(50);
        }

        @TransactionalMethod
        public void releaseForks() {
            leftFork.dec();
            rightFork.dec();
        }

        @TransactionalMethod(maxRetries = 10000)
        public void takeForks() {
            Transaction tx = getThreadLocalTransaction();
            if (leftFork.get() == 1) {
                retry();
            } else {
                leftFork.inc();
            }

            if (rightFork.get() == 1) {
                retry();
            } else {
                rightFork.inc();
            }
        }
    }
}
