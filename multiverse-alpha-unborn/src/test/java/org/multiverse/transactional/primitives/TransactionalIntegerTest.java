package org.multiverse.transactional.primitives;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalIntegerTest {
    private Stm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
    }

    @After
    public void tearDown(){
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        TransactionalInteger ref = new TransactionalInteger(1);

        long version = stm.getVersion();
        ref.inc();

        assertEquals(version+1, stm.getVersion());
        assertEquals(2, ref.get());
    }

    @Test
    public void constructorWithNoArg() {
        TransactionalInteger ref = new TransactionalInteger();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        TransactionalInteger ref = new TransactionalInteger(10);
        assertEquals(10, ref.get());
    }

    @Test
    public void set() {
        TransactionalInteger ref = new TransactionalInteger(10);
        long old = ref.set(100);
        assertEquals(10, old);
        assertEquals(100, ref.get());
    }

    @Test
    public void testInc() {
        TransactionalInteger ref = new TransactionalInteger(100);

        assertEquals(101, ref.inc());
        assertEquals(101, ref.get());

        assertEquals(111, ref.inc(10));
        assertEquals(111, ref.get());

        assertEquals(100, ref.inc(-11));
        assertEquals(100, ref.get());
    }

    @Test
    public void testDec() {
        TransactionalInteger ref = new TransactionalInteger(100);

        assertEquals(99, ref.dec());
        assertEquals(99, ref.get());

        assertEquals(89, ref.dec(10));
        assertEquals(89, ref.get());

        assertEquals(100, ref.dec(-11));
        assertEquals(100, ref.get());
    }

    @Test
    public void testEquals() {
        TransactionalInteger ref1 = new TransactionalInteger(10);
        TransactionalInteger ref2 = new TransactionalInteger(10);
        TransactionalInteger ref3 = new TransactionalInteger(20);

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
        TransactionalInteger ref = new TransactionalInteger(10);
        assertEquals(10, ref.hashCode());

        ref.set(200);
        assertEquals(200, ref.hashCode());
    }

    @Test
    public void testAtomic() {
        TransactionalInteger ref1 = new TransactionalInteger(10);
        TransactionalInteger ref2 = new TransactionalInteger(20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(10, ref1.get());
        assertEquals(20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(TransactionalInteger... refs) {
        for (TransactionalInteger ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }

    @Test
    public void await() {
        TransactionalInteger txInt = new TransactionalInteger();

        SetThread t = new SetThread(txInt, 3);
        t.start();

        txInt.await(3);
    }

    public class SetThread extends TestThread {
        private final TransactionalInteger txInt;
        private final int value;

        public SetThread(TransactionalInteger txInt, int value) {
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
        final TransactionalInteger ref = new TransactionalInteger();

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
