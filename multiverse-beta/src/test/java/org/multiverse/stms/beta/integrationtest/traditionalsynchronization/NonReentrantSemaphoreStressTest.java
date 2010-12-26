package org.multiverse.stms.beta.integrationtest.traditionalsynchronization;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.LockLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;
import org.multiverse.stms.beta.BetaStm;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.newIntRef;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A StressTest that checks if a the Semaphore; a traditional synchronization structure can be build
 * using an STM.
 */
public class NonReentrantSemaphoreStressTest {
    private BetaStm stm;
    private volatile boolean stop;
    private int threadCount = 10;
    private int resourceCount = 5;
    private Semaphore semaphore;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (BetaStm) getGlobalStmInstance();
        stop = false;
    }

    @Test
    public void whenNoLocking() {
        test(LockLevel.LockNone);
    }

    @Test
    public void whenWriteLockReads() {
        test(LockLevel.WriteLockReads);
    }

    @Test
    public void whenWriteLockWrites() {
        test(LockLevel.WriteLockWrites);
    }

    @Test
    public void whenCommitLockReads() {
        test(LockLevel.CommitLockReads);
    }

    @Test
    public void whenCommitLockWrites() {
        test(LockLevel.CommitLockWrites);
    }

    public void test(LockLevel lockLevel) {
        semaphore = new Semaphore(resourceCount, lockLevel);

        WorkerThread[] workers = new WorkerThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            workers[k] = new WorkerThread(k);
        }

        startAll(workers);
        sleepMs(TestUtils.getStressTestDurationMs(30 * 1000));
        System.out.println("Terminating");
        stop = true;
        System.out.println(semaphore.ref.toDebugString());
        joinAll(workers);
    }

    class WorkerThread extends TestThread {
        long count;

        public WorkerThread(int id) {
            super("Producer-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                semaphore.down();
                semaphore.up();
                count++;

                if (count % 1000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }
        }
    }

    class Semaphore {

        private IntRef ref;
        private AtomicLong users = new AtomicLong();
        private AtomicBlock upBlock;
        private AtomicBlock downBlock;

        public Semaphore(int initial, LockLevel lockLevel) {
            ref = newIntRef(initial);
            upBlock = stm.createTransactionFactoryBuilder()
                    .setLockLevel(lockLevel).buildAtomicBlock();
            downBlock = stm.createTransactionFactoryBuilder()
                    .setLockLevel(lockLevel).buildAtomicBlock();
        }

        public void up() {
            upBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    ref.increment();
                }
            });

            users.incrementAndGet();
            if (users.get() > resourceCount) {
                fail();
            }
        }

        public void down() {
            users.decrementAndGet();

            downBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (ref.get() == 0) {
                        retry();
                    }

                    ref.decrement();
                }
            });
        }
    }
}
