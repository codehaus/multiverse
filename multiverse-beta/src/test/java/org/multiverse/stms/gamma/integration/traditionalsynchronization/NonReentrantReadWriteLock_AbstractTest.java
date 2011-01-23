package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Before;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;

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
public abstract class NonReentrantReadWriteLock_AbstractTest {
    protected GammaStm stm;
    private int threadCount = 10;
    private ReadWriteLock readWriteLock;
    private volatile boolean stop;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
        stop = false;
    }

    protected abstract AtomicBlock newReleaseWriteLockBlock();

    protected abstract AtomicBlock newAcquireWriteLockBlock();

    protected abstract AtomicBlock newReleaseReadLockBlock();

    protected abstract AtomicBlock newAcquireReadLockBlock();

    public void run() {
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
        final GammaRef<Long> lock = new GammaRef<Long>(stm, 0L);
        final AtomicLong readers = new AtomicLong();
        final AtomicLong writers = new AtomicLong();
        final AtomicBlock acquireReadLockBlock = newAcquireReadLockBlock();
        final AtomicBlock releaseReadLockBlock = newReleaseReadLockBlock();
        final AtomicBlock acquireWriteLockBlock = newAcquireWriteLockBlock();
        final AtomicBlock releaseWriteLockBlock = newReleaseWriteLockBlock();


        public void acquireReadLock() {
            acquireReadLockBlock.execute(new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    if (lock.get() < 0) {
                        retry();
                    }

                    lock.set(lock.get() + 1);
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

                    lock.set(lock.get() - 1);
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

                    lock.set(-1L);
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

                    lock.set(0L);
                }
            });
        }
    }


}
