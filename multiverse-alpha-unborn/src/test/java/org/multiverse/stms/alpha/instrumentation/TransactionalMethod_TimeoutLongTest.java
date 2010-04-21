package org.multiverse.stms.alpha.instrumentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.transactional.DefaultTransactionalReference;
import org.multiverse.transactional.TransactionalReference;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalMethod_TimeoutLongTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenTimeout() {
        TransactionalReference ref = new DefaultTransactionalReference();

        try {
            tryAwaitNonNull(ref);
            fail();
        } catch (RetryTimeoutException expected) {
        }
    }

    @TransactionalMethod(timeout = 1)
    public void tryAwaitNonNull(TransactionalReference ref) {
        if (ref.isNull()) {
            retry();
        }
    }

    @Test
    public void whenSomeWaitingNeeded() {
        final TransactionalInteger ref = new TransactionalInteger();

        TestThread incThread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                for (int k = 0; k < 100; k++) {
                    ref.inc();
                    sleepMs(50);
                }
            }
        };
        incThread.start();

        tryAwaitFiveSeconds(ref, 50);

        System.out.println("Waiting on incthread to complete");
        joinAll(incThread);
    }

    //we need a high retry count because the reference could be getting a useless value very often

    @TransactionalMethod(timeout = 5, maxRetryCount = 100000)
    public void tryAwaitFiveSeconds(TransactionalInteger ref, int minvalue) {
        if (ref.get() < minvalue) {
            System.out.println("ref.get: " + ref.get() + " and waiting for: " + minvalue);
            retry();
        }
    }


    @Test
    public void multipleWakeupsButNotEnough() {
        final TransactionalInteger ref = new TransactionalInteger();

        TestThread incThread = new TestThread() {
            @Override
            public void doRun() throws Exception {
                for (int k = 0; k < 70; k++) {
                    ref.inc();
                    sleepMs(100);
                }
            }
        };
        incThread.start();

        try {
            tryAwaitFiveSeconds(ref, 100000);
            fail();
        } catch (RetryTimeoutException expected) {
        }

        System.out.println("Waiting on incthread to complete");
        joinAll(incThread);
    }
}