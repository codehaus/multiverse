package org.multiverse.transactional.refs;

import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class ShortRefTest {

    @Test
    public void constructorWithNoArg() {
        ShortRef ref = new ShortRef();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        ShortRef ref = new ShortRef((short) 10);
        assertEquals((char) 10, ref.get());
    }

    @Test
    public void set() {
        ShortRef ref = new ShortRef((short) 10);
        long old = ref.set((short) 100);
        assertEquals((short) 10, old);
        assertEquals((short) 100, ref.get());
    }

    @Test
    public void testInc() {
        ShortRef ref = new ShortRef((short) 100);

        assertEquals((short) 101, ref.inc());
        assertEquals((short) 101, ref.get());

        assertEquals((short) 111, ref.inc((short) 10));
        assertEquals((short) 111, ref.get());

        assertEquals((short) 100, ref.inc((short) -11));
        assertEquals((short) 100, ref.get());
    }

    @Test
    public void testDec() {
        ShortRef ref = new ShortRef((short) 100);

        assertEquals((short) 99, ref.dec());
        assertEquals((short) 99, ref.get());

        assertEquals((short) 89, ref.dec((short) 10));
        assertEquals((short) 89, ref.get());

        assertEquals((short) 100, ref.dec((short) -11));
        assertEquals((short) 100, ref.get());
    }


    @Test
    public void testEquals() {
        ShortRef ref1 = new ShortRef((short) 10);
        ShortRef ref2 = new ShortRef((short) 10);
        ShortRef ref3 = new ShortRef((short) 20);

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
        ShortRef ref = new ShortRef((short) 10);
        assertEquals(10, ref.hashCode());

        ref.set((short) 200);
        assertEquals(200, ref.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("ShortRef(value=10)", new ShortRef((short) 10).toString());
    }

    @Test
    public void testAtomic() {
        ShortRef ref1 = new ShortRef((short) 10);
        ShortRef ref2 = new ShortRef((short) 20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals((short) 10, ref1.get());
        assertEquals((short) 20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(ShortRef... refs) {
        for (ShortRef ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }
}
