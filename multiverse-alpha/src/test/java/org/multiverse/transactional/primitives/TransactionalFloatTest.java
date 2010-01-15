package org.multiverse.transactional.primitives;

import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static java.lang.Float.floatToIntBits;
import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;


public class TransactionalFloatTest {

    @Test
    public void constructorWithNoArg() {
        TransactionalFloat ref = new TransactionalFloat();
        assertFloatEquals(0, ref.get());
    }

    public void assertFloatEquals(float expected, float found) {
        assertEquals(floatToIntBits(expected), floatToIntBits(found));
    }

    @Test
    public void constructorWithSingleArg() {
        TransactionalFloat ref = new TransactionalFloat(10);
        assertFloatEquals(10, ref.get());
    }

    @Test
    public void set() {
        TransactionalFloat ref = new TransactionalFloat();
        Float old = ref.set(100);
        assertFloatEquals(0, old);
        assertFloatEquals(100, ref.get());
    }

    @Test
    public void testInc() {
        TransactionalFloat ref = new TransactionalFloat(100);

        assertFloatEquals(101, ref.inc());
        assertFloatEquals(101, ref.get());

        assertFloatEquals(111, ref.inc(10));
        assertFloatEquals(111, ref.get());

        assertFloatEquals(100, ref.inc(-11));
        assertFloatEquals(100, ref.get());
    }

    @Test
    public void testDec() {
        TransactionalFloat ref = new TransactionalFloat(100);

        assertFloatEquals(99, ref.dec());
        assertFloatEquals(99, ref.get());

        assertFloatEquals(89, ref.dec(10));
        assertFloatEquals(89, ref.get());

        assertFloatEquals(100, ref.dec(-11));
        assertFloatEquals(100, ref.get());
    }


    @Test
    public void testEquals() {
        TransactionalFloat ref1 = new TransactionalFloat(10);
        TransactionalFloat ref2 = new TransactionalFloat(10);
        TransactionalFloat ref3 = new TransactionalFloat(20);

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
        assertEquals(new Float(100).hashCode(), new TransactionalFloat(100).hashCode());
        assertEquals(new Float(10).hashCode(), new TransactionalFloat(10).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("TransactionalFloat(value=10.0)", new TransactionalFloat(10).toString());
    }

    @Test
    public void testAtomic() {
        TransactionalFloat ref1 = new TransactionalFloat(10);
        TransactionalFloat ref2 = new TransactionalFloat(20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertFloatEquals(10, ref1.get());
        assertFloatEquals(20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(TransactionalFloat... refs) {
        for (TransactionalFloat ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }
}
