package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newIntRef;


/**
 * http://en.wikipedia.org/wiki/Dining_philosophers_problem
 */
public class DiningPhilosophersStressTest implements BetaStmConstants {

    private int philosopherCount = 10;
    private volatile boolean stop;

    private BetaIntRef[] forks;
    private BetaStm stm;
    private PessimisticLockLevel pessimisticLockLevel;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
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
    }

    public void assertAllForksHaveReturned() {
        for (BetaIntRef fork : forks) {
            assertEquals(0, fork.atomicGet());
        }
    }

    public PhilosopherThread[] createPhilosopherThreads() {
        PhilosopherThread[] threads = new PhilosopherThread[philosopherCount];
        for (int k = 0; k < philosopherCount; k++) {
            BetaIntRef leftFork = forks[k];
            BetaIntRef rightFork = k == philosopherCount - 1 ? forks[0] : forks[k + 1];
            threads[k] = new PhilosopherThread(k, leftFork, rightFork);
        }
        return threads;
    }

    public void createForks() {
        forks = new BetaIntRef[philosopherCount];
        for (int k = 0; k < forks.length; k++) {
            forks[k] = newIntRef(stm);
        }
    }

    class PhilosopherThread extends TestThread {
        private final BetaIntRef leftFork;
        private final BetaIntRef rightFork;
        private final AtomicBlock releaseForksBlock = stm.createTransactionFactoryBuilder()
                .setPessimisticLockLevel(pessimisticLockLevel)
                .buildAtomicBlock();
        private final AtomicBlock takeForksBlock = stm.createTransactionFactoryBuilder()
                .setPessimisticLockLevel(pessimisticLockLevel)
                .setMaxRetries(10000)
                .buildAtomicBlock();

        PhilosopherThread(int id, BetaIntRef leftFork, BetaIntRef rightFork) {
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

        public void releaseForks() {
            releaseForksBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    leftFork.getAndSet(btx, leftFork.get(btx) - 1);
                    rightFork.getAndSet(btx, rightFork.get(btx) - 1);
                }
            });
        }

        public void takeForks() {
            takeForksBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;

                    if (leftFork.get(btx) == 1) {
                        retry();
                    } else {
                        leftFork.getAndSet(btx, leftFork.get(btx) + 1);
                    }

                    if (rightFork.get(btx) == 1) {
                        retry();
                    } else {
                        rightFork.getAndSet(btx, rightFork.get(btx) + 1);
                    }
                }
            });
        }
    }
}
