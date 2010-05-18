package org.multiverse.transactional.refs;

import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class ByteRefTest {

    @Test
    public void constructorWithNoArg() {
        ByteRef ref = new ByteRef();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        ByteRef ref = new ByteRef((byte) 10);
        assertEquals((byte) 10, ref.get());
    }

    @Test
    public void set() {
        ByteRef ref = new ByteRef();
        byte old = ref.set((byte) 100);
        assertEquals((byte) 0, old);
        assertEquals((byte) 100, ref.get());
    }

    @Test
    public void testInc() {
        ByteRef ref = new ByteRef((byte) 100);

        assertEquals((byte) 101, ref.inc());
        assertEquals((byte) 101, ref.get());

        assertEquals((byte) 111, ref.inc((byte) 10));
        assertEquals((byte) 111, ref.get());

        assertEquals((byte) 100, ref.inc((byte) -11));
        assertEquals((byte) 100, ref.get());
    }

    @Test
    public void testDec() {
        ByteRef ref = new ByteRef((byte) 100);

        assertEquals((byte) 99, ref.dec());
        assertEquals((byte) 99, ref.get());

        assertEquals((byte) 89, ref.dec((byte) 10));
        assertEquals((byte) 89, ref.get());

        assertEquals((byte) 100, ref.dec((byte) -11));
        assertEquals((byte) 100, ref.get());
    }

    @Test
    public void testEquals() {
        ByteRef ref1 = new ByteRef((byte) 10);
        ByteRef ref2 = new ByteRef((byte) 10);
        ByteRef ref3 = new ByteRef((byte) 20);

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
        ByteRef ref = new ByteRef((byte) 10);
        assertEquals(10, ref.hashCode());

        ref.set((byte) 50);
        assertEquals(50, ref.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("ByteRef(value=10)", new ByteRef((byte) 10).toString());
    }

    @Test
    public void testAtomic() {
        ByteRef ref1 = new ByteRef((byte) 10);
        ByteRef ref2 = new ByteRef((byte) 20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertEquals((byte) 10, ref1.get());
        assertEquals((byte) 20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(ByteRef... refs) {
        for (ByteRef ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }

    @Test
    public void awaitTest() {
        final ByteRef ref = new ByteRef();

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                ref.await((byte) 2);
            }
        };

        t.start();
        sleepMs(500);

        assertAlive(t);

        ref.set((byte) 1);
        sleepMs(500);

        assertAlive(t);

        ref.set((byte) 2);
        joinAll(t);
    }
}
