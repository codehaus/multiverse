package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

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
            simpleTestWithWriteSkewAllowed();
        }
    }

    public void simpleTestWithWriteSkewAllowed() {
        IntRef from1 = new IntRef(100);
        IntRef from2 = new IntRef(0);

        IntRef to1 = new IntRef();
        IntRef to2 = new IntRef();

        TransferThread t1 = new TransferThread(from1, from2, to1, true);
        TransferThread t2 = new TransferThread(from2, from1, to2, true);

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
        IntRef from1 = new IntRef(100);
        IntRef from2 = new IntRef(0);

        IntRef to1 = new IntRef(0);
        IntRef to2 = new IntRef(0);

        TransferThread t1 = new TransferThread(from1, from2, to1, false);
        TransferThread t2 = new TransferThread(from2, from1, to2, false);

        t2.start();
        t1.start();

        joinAll(t1, t2);

        clearThreadLocalTransaction();
        int sum = from1.get() + from2.get();
        assertTrue("sumRefs " + sum, sum >= 0);
    }

    private class TransferThread extends TestThread {

        final IntRef from1;
        final IntRef from2;
        final IntRef to;
        final boolean writeSkewAllowed;

        private TransferThread(IntRef from1, IntRef from2, IntRef to, boolean writeSkewAllowed) {
            this.from1 = from1;
            this.from2 = from2;
            this.to = to;
            this.writeSkewAllowed = writeSkewAllowed;
        }

        public void doRun() throws Exception {
            if (writeSkewAllowed) {
                doRunWriteSkewAllowed();
            } else {
                doRunWriteSkewDisallowed();
            }
        }

        @TransactionalMethod(trackReads = true, writeSkew = false)
        public void doRunWriteSkewDisallowed() throws Exception {
            doIt();
        }

        @TransactionalMethod(trackReads = true, writeSkew = true)
        public void doRunWriteSkewAllowed() throws Exception {
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
