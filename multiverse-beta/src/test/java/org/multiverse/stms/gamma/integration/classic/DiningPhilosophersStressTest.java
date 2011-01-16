package org.multiverse.stms.gamma.integration.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.*;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.api.references.RefFactory;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaStmConfiguration;

import static org.junit.Assert.assertFalse;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;


/**
 * http://en.wikipedia.org/wiki/Dining_philosophers_problem
 */
public class DiningPhilosophersStressTest implements GammaConstants {

    private int philosopherCount = 10;
    private volatile boolean stop;

    private BooleanRef[] forks;
    private LockMode lockMode;
    private Stm stm;
    private RefFactory refFactory;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        GammaStmConfiguration config = new GammaStmConfiguration();
        //config.backoffPolicy = new SpinningBackoffPolicy();
        stm = new GammaStm(config);
        refFactory = stm.getRefFactoryBuilder().build();
        stop = false;
    }

    @Test
    public void whenLockNone() {
        test(LockMode.None);
    }

    @Test
    public void testReadLock() {
        test(LockMode.Read);
    }

    @Test
    public void testWriteLock() {
        test(LockMode.Write);
    }

    @Test
    public void testCommitLock() {
        test(LockMode.Commit);
    }

    public void test(LockMode lockLevel) {
        this.lockMode = lockLevel;
        createForks();

        PhilosopherThread[] philosopherThreads = createPhilosopherThreads();
        startAll(philosopherThreads);

        sleepMs(getStressTestDurationMs(30 * 1000));

        stop = true;
        joinAll(philosopherThreads);

        assertAllForksHaveReturned();

        for (PhilosopherThread philosopherThread : philosopherThreads) {
            System.out.printf("%s ate %s times\n",
                    philosopherThread.getName(), philosopherThread.eatCount);
        }
    }

    public void assertAllForksHaveReturned() {
        for (BooleanRef fork : forks) {
            assertFalse(fork.atomicGet());
        }
    }

    public PhilosopherThread[] createPhilosopherThreads() {
        PhilosopherThread[] threads = new PhilosopherThread[philosopherCount];
        for (int k = 0; k < philosopherCount; k++) {
            BooleanRef leftFork = forks[k];
            BooleanRef rightFork = k == philosopherCount - 1 ? forks[0] : forks[k + 1];
            threads[k] = new PhilosopherThread(k, leftFork, rightFork);
        }
        return threads;
    }

    public void createForks() {
        forks = new BooleanRef[philosopherCount];
        for (int k = 0; k < forks.length; k++) {
            forks[k] = refFactory.newBooleanRef(false);
        }
    }

    class PhilosopherThread extends TestThread {
        private int eatCount = 0;
        private final BooleanRef leftFork;
        private final BooleanRef rightFork;
        private final AtomicBlock releaseForksBlock = stm.createTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();
        private final AtomicBlock takeForksBlock = stm.createTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .setMaxRetries(10000)
                .buildAtomicBlock();

        PhilosopherThread(int id, BooleanRef leftFork, BooleanRef rightFork) {
            super("PhilosopherThread-" + id);
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        @Override
        public void doRun() {
            while (!stop) {
                eatCount++;
                if (eatCount % 100 == 0) {
                    System.out.printf("%s at %s\n", getName(), eatCount);
                }
                eat();
                //   sleepMs(5);
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

        public void releaseForks() {
            releaseForksBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    leftFork.set(false);
                    rightFork.set(false);

                }
            });
        }

        public void takeForks() {
            takeForksBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (leftFork.get() || rightFork.get()) {
                        retry();
                    }

                    leftFork.set(true);
                    rightFork.set(true);
                }
            });
        }
    }
}
