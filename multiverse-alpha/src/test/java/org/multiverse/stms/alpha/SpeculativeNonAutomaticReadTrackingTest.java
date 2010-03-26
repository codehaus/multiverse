package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.readonly.MonoReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MonoUpdateAlphaTransaction;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertInstanceOf;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class SpeculativeNonAutomaticReadTrackingTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadonlyAndAutomaticReadtrackingNeeded() {
        SpeculativeNonAutomaticReadTracking o = new SpeculativeNonAutomaticReadTracking();
        o.set(1);
        new DelayedSetThread(o).start();
        o.getZeroOrWait();

        //there are 3 transactions. 1) for non tracking, 2 for tracking but retry failure
        //and 3) for a successfull get.
        assertEquals(3, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(1), MonoReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(2), MonoReadonlyAlphaTransaction.class);

        //make sure that the system learned.
        SpeculativeNonAutomaticReadTracking o2 = new SpeculativeNonAutomaticReadTracking();
        o2.set(1);
        new DelayedSetThread(o2).start();

        o2.getZeroOrWait();
        assertEquals(2, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), MonoReadonlyAlphaTransaction.class);
        assertInstanceOf(o2.transactions.get(1), MonoReadonlyAlphaTransaction.class);
    }

    @Test
    public void whenReadonlyAndNoAutomaticReadtrackingNeeded() {
        SpeculativeNonAutomaticReadTracking o = new SpeculativeNonAutomaticReadTracking();
        o.getZeroOrFail();

        assertEquals(1, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);

        //and make sure that nothing has changed
        SpeculativeNonAutomaticReadTracking o2 = new SpeculativeNonAutomaticReadTracking();
        o2.getZeroOrFail();
        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
    }

    @Test
    public void whenUpdateAndAutomaticReadtrackingNeeded() {
        SpeculativeNonAutomaticReadTracking o = new SpeculativeNonAutomaticReadTracking();
        o.set(1);
        new DelayedSetThread(o).start();
        o.setOneIfZeroOrWait();

        assertEquals(4, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(1), MonoReadonlyAlphaTransaction.class);
        //this is the one after the wait
        assertInstanceOf(o.transactions.get(2), MonoReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(3), MonoUpdateAlphaTransaction.class);

        //make sure that the system learned.
        SpeculativeNonAutomaticReadTracking o2 = new SpeculativeNonAutomaticReadTracking();
        o2.set(1);
        new DelayedSetThread(o2).start();

        o2.setOneIfZeroOrWait();
        assertEquals(2, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), MonoUpdateAlphaTransaction.class);
        assertInstanceOf(o2.transactions.get(1), MonoUpdateAlphaTransaction.class);
    }

    @Test
    public void whenUpdateAndNoAutomaticReadTrackingNeeded() {
        SpeculativeNonAutomaticReadTracking o = new SpeculativeNonAutomaticReadTracking();
        o.setOneIfZeroOrFail();

        assertEquals(2, o.transactions.size());
        assertInstanceOf(o.transactions.get(0), NonTrackingReadonlyAlphaTransaction.class);
        assertInstanceOf(o.transactions.get(1), MonoUpdateAlphaTransaction.class);

        //make sure that the system learned.
        SpeculativeNonAutomaticReadTracking o2 = new SpeculativeNonAutomaticReadTracking();
        new DelayedSetThread(o2).start();

        o2.setOneIfZeroOrFail();
        assertEquals(1, o2.transactions.size());
        assertInstanceOf(o2.transactions.get(0), MonoUpdateAlphaTransaction.class);
    }

    @TransactionalObject
    class SpeculativeNonAutomaticReadTracking {
        private int value;

        @Exclude
        private List<Transaction> transactions = new LinkedList<Transaction>();

        public void set(int value) {
            this.value = value;
        }

        public void getZeroOrFail() {
            transactions.add(getThreadLocalTransaction());
            if (value != 0) {
                throw new RuntimeException();
            }
        }

        public void getZeroOrWait() {
            transactions.add(getThreadLocalTransaction());
            if (value != 0) {
                retry();
            }
        }

        public void setOneIfZeroOrFail() {
            transactions.add(getThreadLocalTransaction());

            if (value != 0) {
                throw new RuntimeException();
            } else {
                value = 1;
            }
        }

        public void setOneIfZeroOrWait() {
            transactions.add(getThreadLocalTransaction());

            if (value != 0) {
                retry();
            } else {
                value = 1;
            }
        }
    }

    public class DelayedSetThread extends TestThread {
        private final SpeculativeNonAutomaticReadTracking o;

        public DelayedSetThread(SpeculativeNonAutomaticReadTracking o) {
            this.o = o;
        }

        @Override
        public void doRun() throws Exception {
            sleepMs(500);
            o.set(0);
        }
    }
}
