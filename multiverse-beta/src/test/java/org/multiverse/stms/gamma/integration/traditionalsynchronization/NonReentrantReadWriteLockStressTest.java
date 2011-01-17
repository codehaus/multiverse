package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Stresstest that sees if the stm can be used to create a readwritelock that is not reentrant.
 *
 * @author Peter Veentjer.
 */
public class NonReentrantReadWriteLockStressTest {
    private GammaStm stm;
    private int threadCount = 10;
    private ReadWriteLock readWriteLock;
    private volatile boolean stop;
    private LockMode lockMode;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
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
        this.lockMode = lockMode;
        readWriteLock = new ReadWriteLock();

        StressThread[] threads = new StressThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new StressThread(k);
        }

        startAll(threads);
        sleepMs(30000);
        stop = true;
        joinAll(threads);
    }

    class StressThread extends TestThread {
        public StressThread(int id) {
            super("StressThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            long count = 0;
            while (!stop) {
                if (randomOneOf(5)) {
                    readWriteLock.acquireWriteLock();
                    sleepMs(100);
                    readWriteLock.releaseWriteLock();
                } else {
                    readWriteLock.acquireReadLock();
                    sleepMs(100);
                    readWriteLock.releaseReadLock();
                }
                count++;
                if (count % 10 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
            }
        }
    }

    class ReadWriteLock {
        final GammaLongRef lock = new GammaLongRef(stm);
        final AtomicLong readers = new AtomicLong();
        final AtomicLong writers = new AtomicLong();
        final AtomicBlock acquireReadLockBlock = stm.newTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();
        final AtomicBlock releaseReadLockBlock = stm.newTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();
        final AtomicBlock acquireWriteLockBlock = stm.newTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();
        final AtomicBlock releaseWriteLockBlock = stm.newTransactionFactoryBuilder()
                .setReadLockMode(lockMode)
                .buildAtomicBlock();


        public void acquireReadLock() {
            acquireReadLockBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (lock.get() < 0) {
                        retry();
                    }

                    lock.increment();
                }
            });

            readers.incrementAndGet();

            assertEquals(0, writers.get());
        }

        public void releaseReadLock() {
            readers.decrementAndGet();
            assertEquals(0, writers.get());

            releaseReadLockBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (lock.get() <= 0) {
                        throw new IllegalMonitorStateException();
                    }

                    lock.decrement();
                }
            });
        }

        public void acquireWriteLock() {
            acquireWriteLockBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (lock.get() != 0) {
                        retry();
                    }

                    lock.set(-1);
                }
            });

            writers.incrementAndGet();
            assertEquals(0, readers.get());
        }

        public void releaseWriteLock() {
            writers.decrementAndGet();
            assertEquals(0, readers.get());

            releaseWriteLockBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (lock.get() != -1) {
                        throw new IllegalMonitorStateException();
                    }

                    lock.set(0);
                }
            });
        }
    }
}
