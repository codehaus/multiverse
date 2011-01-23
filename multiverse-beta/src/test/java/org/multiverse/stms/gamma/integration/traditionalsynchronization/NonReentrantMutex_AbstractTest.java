package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Before;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A stresstest that checks if the NonReentrantMutex; a traditional synchronization structure, can be build
 * using stm. It isn't meant as a replacement for Mutex, but just to see if the system behaves like it should.
 *
 * @author Peter Veentjer.
 */
public abstract class NonReentrantMutex_AbstractTest {

    private volatile boolean stop;
    private int accountCount = 50;

    private int threadCount = processorCount() * 4;
    private ProtectedIntValue[] intValues;
    protected GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
        stop = false;
    }

    protected abstract AtomicBlock newUnlockBlock();

    protected abstract AtomicBlock newLockBlock();

    public void run() {
        intValues = new ProtectedIntValue[accountCount];
        for (int k = 0; k < accountCount; k++) {
            intValues[k] = new ProtectedIntValue();
        }

        IncThread[] threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k);
        }

        startAll(threads);
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        assertEquals(sum(threads), sum(intValues));
        System.out.println("total increments: " + sum(threads));
    }

    int sum(IncThread[] threads) {
        int result = 0;
        for (IncThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    int sum(ProtectedIntValue[] intValues) {
        int result = 0;
        for (ProtectedIntValue intValue : intValues) {
            result += intValue.balance;
        }
        return result;
    }

    class IncThread extends TestThread {
        private int count;

        public IncThread(int id) {
            super("IncThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                ProtectedIntValue intValue = intValues[TestUtils.randomInt(accountCount)];
                intValue.inc();

                if (count % 500000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }

                count++;
            }
        }
    }

    class ProtectedIntValue {
        final NonReentrantMutex mutex = new NonReentrantMutex();

        int balance;

        public void inc() {
            mutex.lock();
            balance++;
            mutex.unlock();
        }
    }

    class NonReentrantMutex {
        final GammaRef locked = new GammaRef(stm, null);
        final AtomicBlock lockBlock = newLockBlock();
        final AtomicBlock unlockBlock = newUnlockBlock();

        final AtomicVoidClosure lockClosure = new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                locked.awaitNull(tx);
                locked.set(tx, this);
            }
        };

        final AtomicVoidClosure unlockClosure = new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                if (locked.isNull(tx)) {
                    throw new IllegalStateException();
                }

                locked.set(tx, null);
            }
        };

        public void lock() {
            lockBlock.execute(lockClosure);
        }

        public void unlock() {
            unlockBlock.execute(unlockClosure);
        }
    }
}
