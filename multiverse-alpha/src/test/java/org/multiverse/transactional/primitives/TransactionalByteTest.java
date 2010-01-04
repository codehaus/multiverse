package org.multiverse.transactional.primitives;

import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalByteTest {

    @Test
    public void constructorWithNoArg() {
        TransactionalByte ref = new TransactionalByte();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        TransactionalByte ref = new TransactionalByte((byte) 10);
        assertEquals((byte) 10, ref.get());
    }

    @Test
    public void set() {
        TransactionalByte ref = new TransactionalByte();
        byte old = ref.set((byte) 100);
        assertEquals((byte) 0, old);
        assertEquals((byte) 100, ref.get());
    }

    @Test
    public void testInc() {
        TransactionalByte ref = new TransactionalByte((byte) 100);

        assertEquals((byte) 101, ref.inc());
        assertEquals((byte) 101, ref.get());

        assertEquals((byte) 111, ref.inc((byte) 10));
        assertEquals((byte) 111, ref.get());

        assertEquals((byte) 100, ref.inc((byte) -11));
        assertEquals((byte) 100, ref.get());
    }

    @Test
    public void testDec() {
        TransactionalByte ref = new TransactionalByte((byte) 100);

        assertEquals((byte) 99, ref.dec());
        assertEquals((byte) 99, ref.get());

        assertEquals((byte) 89, ref.dec((byte) 10));
        assertEquals((byte) 89, ref.get());

        assertEquals((byte) 100, ref.dec((byte) -11));
        assertEquals((byte) 100, ref.get());
    }

    @Test
    public void testEquals() {
        TransactionalByte ref1 = new TransactionalByte((byte) 10);
        TransactionalByte ref2 = new TransactionalByte((byte) 10);
        TransactionalByte ref3 = new TransactionalByte((byte) 20);

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
        TransactionalByte ref = new TransactionalByte((byte) 10);
        assertEquals(10, ref.hashCode());

        ref.set((byte) 50);
        assertEquals(50, ref.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("TransactionalByte(value=10)", new TransactionalByte((byte) 10).toString());
    }

    @Test
    public void testAtomic() {
        TransactionalByte ref1 = new TransactionalByte((byte) 10);
        TransactionalByte ref2 = new TransactionalByte((byte) 20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertEquals((byte) 10, ref1.get());
        assertEquals((byte) 20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(TransactionalByte... refs) {
        for (TransactionalByte ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }

}
