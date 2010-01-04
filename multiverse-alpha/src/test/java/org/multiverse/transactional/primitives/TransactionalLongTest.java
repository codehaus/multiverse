package org.multiverse.transactional.primitives;

import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalLongTest {

    @Test
    public void constructorWithNoArg() {
        TransactionalLong ref = new TransactionalLong();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        TransactionalLong ref = new TransactionalLong(10);
        assertEquals(10L, ref.get());
    }

    @Test
    public void set() {
        TransactionalLong ref = new TransactionalLong();
        long old = ref.set(100L);
        assertEquals(0L, old);
        assertEquals(100L, ref.get());
    }

    @Test
    public void testInc() {
        TransactionalLong ref = new TransactionalLong(100);

        assertEquals(101, ref.inc());
        assertEquals(101, ref.get());

        assertEquals(111, ref.inc(10));
        assertEquals(111, ref.get());

        assertEquals(100, ref.inc(-11));
        assertEquals(100, ref.get());
    }

    @Test
    public void testDec() {
        TransactionalLong ref = new TransactionalLong(100);

        assertEquals(99, ref.dec());
        assertEquals(99, ref.get());

        assertEquals(89, ref.dec(10));
        assertEquals(89, ref.get());

        assertEquals(100, ref.dec(-11));
        assertEquals(100, ref.get());
    }


    @Test
    public void testEquals() {
        TransactionalLong ref1 = new TransactionalLong(10);
        TransactionalLong ref2 = new TransactionalLong(10);
        TransactionalLong ref3 = new TransactionalLong(20);

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
        TransactionalLong ref = new TransactionalLong(10);
        assertEquals(new Long(10).hashCode(), ref.hashCode());

        ref.set(200);
        assertEquals(new Long(200).hashCode(), ref.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("TransactionalLong(value=10)", new TransactionalLong(10).toString());
    }

    @Test
    public void testAtomic() {
        TransactionalLong ref1 = new TransactionalLong(10);
        TransactionalLong ref2 = new TransactionalLong(20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(10, ref1.get());
        assertEquals(20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(TransactionalLong... refs) {
        for (TransactionalLong ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }

}
