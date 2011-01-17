package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A StressTest that checks if a the Semaphore; a traditional synchronization structure can be build
 * using an STM.
 */
public class NonReentrantSemaphoreStressTest {
    private GammaStm stm;
    private volatile boolean stop;
    private int threadCount = 10;
    private int resourceCount = 5;
    private Semaphore semaphore;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
        stop = false;
    }

    @Test
    public void testNoLocking() {
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
    public void testExclusiveLock() {
        test(LockMode.Exclusive);
    }

    public void test(LockMode lockMode) {
        semaphore = new Semaphore(resourceCount, lockMode);

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

        private LongRef ref;
        private AtomicLong users = new AtomicLong();
        private AtomicBlock upBlock;
        private AtomicBlock downBlock;

        public Semaphore(int initial, LockMode lockMode) {
            ref = new GammaLongRef(stm, initial);
            upBlock = stm.newTransactionFactoryBuilder()
                    .setReadLockMode(lockMode)
                    .buildAtomicBlock();
            downBlock = stm.newTransactionFactoryBuilder()
                    .setReadLockMode(lockMode)
                    .buildAtomicBlock();
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
