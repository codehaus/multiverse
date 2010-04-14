package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Test that checks when writeSkew detection has been disabled and can happen, and with writeskewdetection enabled
 * that it can't happen.
 *
 * @author Peter Veentjer.
 */
public class WriteSkewStressTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void simpleTestWithWriteSkewDetectionDisabledRepeated() {
        for (int k = 0; k < 100; k++) {
            simpleTestWithEnabledWriteSkewProblem();
        }
    }

    public void simpleTestWithEnabledWriteSkewProblem() {
        TransactionalInteger from1 = new TransactionalInteger(100);
        TransactionalInteger from2 = new TransactionalInteger(0);

        TransactionalInteger to1 = new TransactionalInteger();
        TransactionalInteger to2 = new TransactionalInteger();

        AnotherTransferThread t1 = new AnotherTransferThread(from1, from2, to1, true);
        AnotherTransferThread t2 = new AnotherTransferThread(from2, from1, to2, true);

        //there is a big fat delay before the tx completes, so would but both threads in the problem area.
        t1.start();
        t2.start();

        joinAll(t1, t2);
        int sum = from1.get() + from2.get();
        assertTrue(sum < 0);
    }

    @Test
    public void simpleWithoutWriteSkew() {
        for (int k = 0; k < 100; k++) {
            simpleTestWithoutWriteSkew();
        }
    }

    public void simpleTestWithoutWriteSkew() {
        TransactionalInteger from1 = new TransactionalInteger(100);
        TransactionalInteger from2 = new TransactionalInteger(0);

        TransactionalInteger to1 = new TransactionalInteger(0);
        TransactionalInteger to2 = new TransactionalInteger(0);

        AnotherTransferThread t1 = new AnotherTransferThread(from1, from2, to1, false);
        AnotherTransferThread t2 = new AnotherTransferThread(from2, from1, to2, false);

        t2.start();
        t1.start();

        joinAll(t1, t2);

        clearThreadLocalTransaction();
        int sum = from1.get() + from2.get();
        assertTrue("sum " + sum, sum >= 0);
    }

    private class AnotherTransferThread extends TestThread {

        final TransactionalInteger from1;
        final TransactionalInteger from2;
        final TransactionalInteger to;
        final boolean writeSkewProblemAllowed;

        private AnotherTransferThread(TransactionalInteger from1, TransactionalInteger from2, TransactionalInteger to, boolean writeSkewProblemAllowed) {
            this.from1 = from1;
            this.from2 = from2;
            this.to = to;
            this.writeSkewProblemAllowed = writeSkewProblemAllowed;
        }

        public void doRun() throws Exception {
            if (writeSkewProblemAllowed) {
                doRunWriteSkewProblemAllowed();
            } else {
                doRunWriteSkewProblemDisallowed();
            }
        }

        @TransactionalMethod(automaticReadTrackingEnabled = true, writeSkewProblemAllowed = false)
        public void doRunWriteSkewProblemDisallowed() throws Exception {
            doIt();
        }

        @TransactionalMethod(automaticReadTrackingEnabled = true, writeSkewProblemAllowed = true)
        public void doRunWriteSkewProblemAllowed() throws Exception {
            doIt();
        }

        private void doIt() {
            if (from1.get() + from2.get() >= 100) {
                from1.dec(100);
                to.inc(100);
            }

            sleepMs(200);
        }
    }
}
