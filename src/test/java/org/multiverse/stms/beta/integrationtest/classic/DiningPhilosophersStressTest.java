package org.multiverse.stms.beta.integrationtest.classic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.IntRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createIntRef;
import static org.multiverse.stms.beta.ThreadLocalBetaObjectPool.getThreadLocalBetaObjectPool;


/**
 * todo: improvement: print how many times each philosopher eat.
 */
public class DiningPhilosophersStressTest {

    private int philosopherCount = 10;
    private volatile boolean stop;

    private IntRef[] forks;
    private BetaStm stm;
    private boolean pessimistic;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        stop = false;
    }

    @Test
    public void testPessimistic() {
        test(true);
    }

    @Test
    public void testOptimistic() {
        test(false);
    }

    public void test(boolean pessimistic) {
        this.pessimistic = pessimistic;
        createForks();

        PhilosopherThread[] philosopherThreads = createPhilosopherThreads();
        startAll(philosopherThreads);

        sleepMs(getStressTestDurationMs(30 * 1000));

        stop = true;
        joinAll(philosopherThreads);

        assertAllForksHaveReturned();
    }

    public void assertAllForksHaveReturned() {
        for (IntRef fork : forks) {
            assertEquals(0, fork.___unsafeLoad().value);
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
            forks[k] = createIntRef(stm);
        }
    }

    class PhilosopherThread extends TestThread {
        private final IntRef leftFork;
        private final IntRef rightFork;
        private final AtomicBlock releaseForksBlock = stm.getTransactionFactoryBuilder()
                .setPessimisticLockLevel(pessimistic ? PessimisticLockLevel.Read : PessimisticLockLevel.None)
                .buildAtomicBlock();
        private final AtomicBlock takeForksBlock = stm.getTransactionFactoryBuilder()
                .setPessimisticLockLevel(pessimistic ? PessimisticLockLevel.Read : PessimisticLockLevel.None)
                .setMaxRetries(10000)
                .buildAtomicBlock();

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

        public void releaseForks() {
            releaseForksBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    BetaTransaction btx = (BetaTransaction) tx;
                    leftFork.set(btx, pool, leftFork.get(btx, pool) - 1);
                    rightFork.set(btx, pool, rightFork.get(btx, pool) - 1);
                }
            });
        }

        public void takeForks() {
            takeForksBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaObjectPool pool = getThreadLocalBetaObjectPool();
                    BetaTransaction btx = (BetaTransaction) tx;

                    if (leftFork.get(btx, pool) == 1) {
                        retry();
                    } else {
                        leftFork.set(btx, pool, leftFork.get(btx, pool) + 1);
                    }

                    if (rightFork.get(btx, pool) == 1) {
                        retry();
                    } else {
                        rightFork.set(btx, pool, rightFork.get(btx, pool) + 1);
                    }
                }
            });
        }
    }
}
