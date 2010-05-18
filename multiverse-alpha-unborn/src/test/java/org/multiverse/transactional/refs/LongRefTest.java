package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class LongRefTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void constructorWithNoArg() {
        LongRef ref = new LongRef();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        LongRef ref = new LongRef(10);
        assertEquals(10, ref.get());
    }

    @Test
    public void set() {
        LongRef ref = new LongRef(10);
        long old = ref.set(100);
        assertEquals(10, old);
        assertEquals(100, ref.get());
    }

    @Test
    public void testInc() {
        LongRef ref = new LongRef(100);

        assertEquals(101, ref.inc());
        assertEquals(101, ref.get());

        assertEquals(111, ref.inc(10));
        assertEquals(111, ref.get());

        assertEquals(100, ref.inc(-11));
        assertEquals(100, ref.get());
    }

    @Test
    public void testDec() {
        LongRef ref = new LongRef(100);

        assertEquals(99, ref.dec());
        assertEquals(99, ref.get());

        assertEquals(89, ref.dec(10));
        assertEquals(89, ref.get());

        assertEquals(100, ref.dec(-11));
        assertEquals(100, ref.get());
    }

    @Test
    public void testEquals() {
        LongRef ref1 = new LongRef(10);
        LongRef ref2 = new LongRef(10);
        LongRef ref3 = new LongRef(20);

        assertFalse(ref1.equals(null));
        assertFalse(ref1.equals(""));
        assertTrue(ref1.equals(ref2));
        assertTrue(ref2.equals(ref1));
        assertTrue(ref1.equals(ref1));
        assertFalse(ref1.equals(ref3));
        assertFalse(ref3.equals(ref1));
    }

    @Test
    public void testHashCode() {
        LongRef ref = new LongRef(10);
        assertEquals(10, ref.hashCode());

        ref.set(200);
        assertEquals(200, ref.hashCode());
    }

    @Test
    public void testAtomic() {
        LongRef ref1 = new LongRef(10);
        LongRef ref2 = new LongRef(20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(10, ref1.get());
        assertEquals(20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(LongRef... refs) {
        for (LongRef ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }

    @Test
    public void await() {
        LongRef txInt = new LongRef();

        SetThread t = new SetThread(txInt, 3);
        t.start();

        txInt.await(3);
    }

    public class SetThread extends TestThread {
        private final LongRef txInt;
        private final int value;

        public SetThread(LongRef txInt, int value) {
            super("SetThread");
            this.txInt = txInt;
            this.value = value;
        }

        @Override
        public void doRun() throws Exception {
            sleepMs(300);
            txInt.set(value);
        }
    }

    @Test
    public void awaitTest() {
        final LongRef ref = new LongRef();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                ref.await(2);
            }
        };

        t.start();
        sleepMs(500);

        assertAlive(t);

        ref.set(1);
        sleepMs(500);

        assertAlive(t);

        ref.set(2);
        joinAll(t);
    }
}
