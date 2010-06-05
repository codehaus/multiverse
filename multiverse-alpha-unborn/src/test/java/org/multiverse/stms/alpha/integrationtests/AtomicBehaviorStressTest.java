package org.multiverse.stms.alpha.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * A test that checks if modifications are done atomically. So a transactions that are aborted, should
 * not be committed (not even partially) to the heap.
 * <p/>
 * The test: there is a modification thread that updates an integervalue. The only valid value that is permitted
 * in the heap is a value that can be divided by 2. The update is done in 2 staps that increase the value by one
 * and in some cases the transaction is aborted.
 *
 * @author Peter Veentjer.
 */
public class AtomicBehaviorStressTest {

    private IntRef ref;
    private volatile boolean stop;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        ref = new IntRef(0);
        stop = true;
    }

    @Test
    public void test() {
        ModifyThread modifyThread = new ModifyThread(0);
        ObserverThread observerThread = new ObserverThread();

        startAll(modifyThread, observerThread);
        sleepMs(TestUtils.getStressTestDurationMs(20*1000));
        stop = true;
        joinAll(modifyThread, observerThread);
    }

    class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        public void doRun() {
            while (!stop) {
                try {
                    doit();
                } catch (DeadTransactionException ignore) {
                }
            }
        }

        @TransactionalMethod
        public void doit() {
            if (ref.get() % 2 != 0) {
                fail();
            }

            ref.inc();

            sleepRandomMs(20);

            if (randomBoolean()) {
                getThreadLocalTransaction().abort();
            } else {
                ref.inc();
            }
        }
    }

    class ObserverThread extends TestThread {
        public ObserverThread() {
            super("ObserverThread");
        }

        @Override
        public void doRun() {
            while (!stop) {
                doit();
                sleepRandomMs(5);
            }
        }

        @TransactionalMethod
        public void doit() {
            if (ref.get() % 2 != 0) {
                fail();
            }
        }
    }
}
