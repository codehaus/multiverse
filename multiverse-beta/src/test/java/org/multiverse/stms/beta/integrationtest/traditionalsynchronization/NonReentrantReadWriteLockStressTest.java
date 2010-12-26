package org.multiverse.stms.beta.integrationtest.traditionalsynchronization;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.references.IntRef;
import org.multiverse.stms.beta.BetaStm;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Stresstest that sees if the stm can be used to create a readwritelock that is not reentrant.
 *
 * @author Peter Veentjer.
 */
public class NonReentrantReadWriteLockStressTest {
    private BetaStm stm;
    private int threadCount = 10;
    private ReadWriteLock readWriteLock;
    private volatile boolean stop;

    @Before
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
        stop = false;
    }

    @Test
    public void test() {
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
        final IntRef lock = newIntRef();
        final AtomicLong readers = new AtomicLong();
        final AtomicLong writers = new AtomicLong();

        public void acquireReadLock() {
            execute(new AtomicVoidClosure() {
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

            execute(new AtomicVoidClosure() {
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
            execute(new AtomicVoidClosure() {
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

            execute(new AtomicVoidClosure() {
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
