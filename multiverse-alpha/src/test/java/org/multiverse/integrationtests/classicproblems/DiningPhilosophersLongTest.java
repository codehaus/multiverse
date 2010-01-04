package org.multiverse.integrationtests.classicproblems;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * The cause of the dining philosophers problem is that the take of the left and right fork are not atomic. So it
 * could happen that all philosopers have their left fork, but won't get the right for because the philosopher sitting
 * right to them has that fork.
 * <p/>
 * Within the MultiversionedStm both forks are aquired atomically (so a philosopher gets them both, or won't
 * get them at all).
 *
 * @author Peter Veentjer.
 */
public class DiningPhilosophersLongTest {
    private int philosopherCount = 10;
    private int eatCount = 1000;

    private TransactionalInteger[] forks;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //   stm.getProfiler().print();
    }

    @Test
    public void test() {
        createForks();

        PhilosopherThread[] philosopherThreads = createPhilosopherThreads();
        startAll(philosopherThreads);
        joinAll(philosopherThreads);

        assertAllForksHaveReturned();
    }

    public void assertAllForksHaveReturned() {
        for (TransactionalInteger fork : forks) {
            assertEquals(0, fork.get());
        }
    }

    public PhilosopherThread[] createPhilosopherThreads() {
        PhilosopherThread[] threads = new PhilosopherThread[philosopherCount];
        for (int k = 0; k < philosopherCount; k++) {
            TransactionalInteger leftFork = forks[k];
            TransactionalInteger rightFork = k == philosopherCount - 1 ? forks[0] : forks[k + 1];
            threads[k] = new PhilosopherThread(k, leftFork, rightFork);
        }
        return threads;
    }

    public void createForks() {
        forks = new TransactionalInteger[philosopherCount];
        for (int k = 0; k < forks.length; k++) {
            forks[k] = new TransactionalInteger(0);
        }
    }

    class PhilosopherThread extends TestThread {
        private final TransactionalInteger leftFork;
        private final TransactionalInteger rightFork;

        PhilosopherThread(int id, TransactionalInteger leftFork, TransactionalInteger rightFork) {
            super("PhilosopherThread-" + id);
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        @Override
        public void doRun() {
            for (int k = 0; k < eatCount; k++) {
                if (k % 100 == 0) {
                    System.out.printf("%s at %s\n", getName(), k);
                }
                eat();
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

        @TransactionalMethod(retryCount = 10000)
        public void takeForks() {
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
