package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.BooleanRef;
import org.multiverse.api.references.RefFactory;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.assertFalse;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;


/**
 * http://en.wikipedia.org/wiki/Dining_philosophers_problem
 */
public class DiningPhilosophersStressTest implements BetaStmConstants {

    private int philosopherCount = 10;
    private volatile boolean stop;

    private BooleanRef[] forks;
    private PessimisticLockLevel pessimisticLockLevel;
    private Stm stm;
    private RefFactory refFactory;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        BetaStmConfiguration config = new BetaStmConfiguration();
        //config.backoffPolicy = new SpinningBackoffPolicy();
        stm = new BetaStm(config);
        refFactory = stm.getReferenceFactoryBuilder().build();
        stop = false;
    }

    @Test
    public void whenLockNone() {
        test(PessimisticLockLevel.LockNone);
    }

    @Test
    public void testPrivatizeReads() {
        test(PessimisticLockLevel.PrivatizeReads);
    }

    @Test
    public void testPrivatizeWrite() {
        test(PessimisticLockLevel.PrivatizeWrites);
    }

    @Test
    public void testEnsureReads() {
        test(PessimisticLockLevel.EnsureReads);
    }

    @Test
    public void testEnsureWrite() {
        test(PessimisticLockLevel.EnsureWrites);
    }

    public void test(PessimisticLockLevel pessimisticLockLevel) {
        this.pessimisticLockLevel = pessimisticLockLevel;
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
                .setPessimisticLockLevel(pessimisticLockLevel)
                .buildAtomicBlock();
        private final AtomicBlock takeForksBlock = stm.createTransactionFactoryBuilder()
                .setPessimisticLockLevel(pessimisticLockLevel)
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