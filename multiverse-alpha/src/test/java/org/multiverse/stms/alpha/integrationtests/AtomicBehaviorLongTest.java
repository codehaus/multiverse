package org.multiverse.stms.alpha.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
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
public class AtomicBehaviorLongTest {

    private Stm stm;

    private TransactionalInteger ref;
    private int modifyCount = 500;
    private AtomicInteger modifyCountDown = new AtomicInteger();

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
        ref = new TransactionalInteger(0);
    }

    @Test
    public void test() {
        modifyCountDown.set(modifyCount);

        ModifyThread modifyThread = new ModifyThread(0);
        ObserverThread observerThread = new ObserverThread();

        startAll(modifyThread, observerThread);
        joinAll(modifyThread, observerThread);
    }

    class ModifyThread extends TestThread {
        public ModifyThread(int id) {
            super("ModifyThread-" + id);
        }

        public void doRun() {
            while (modifyCountDown.getAndDecrement() > 0) {
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
            while (modifyCountDown.get() > 0) {
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
