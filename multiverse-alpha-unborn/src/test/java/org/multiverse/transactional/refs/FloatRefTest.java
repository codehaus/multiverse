package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static java.lang.Float.floatToIntBits;
import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;


public class FloatRefTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void constructorWithNoArg() {
        FloatRef ref = new FloatRef();
        assertFloatEquals(0, ref.get());
    }

    public void assertFloatEquals(float expected, float found) {
        assertEquals(floatToIntBits(expected), floatToIntBits(found));
    }

    @Test
    public void constructorWithSingleArg() {
        FloatRef ref = new FloatRef(10);
        assertFloatEquals(10, ref.get());
    }

    @Test
    public void set() {
        FloatRef ref = new FloatRef();
        Float old = ref.set(100);
        assertFloatEquals(0, old);
        assertFloatEquals(100, ref.get());
    }

    @Test
    public void testInc() {
        FloatRef ref = new FloatRef(100);

        assertFloatEquals(101, ref.inc());
        assertFloatEquals(101, ref.get());

        assertFloatEquals(111, ref.inc(10));
        assertFloatEquals(111, ref.get());

        assertFloatEquals(100, ref.inc(-11));
        assertFloatEquals(100, ref.get());
    }

    @Test
    public void testDec() {
        FloatRef ref = new FloatRef(100);

        assertFloatEquals(99, ref.dec());
        assertFloatEquals(99, ref.get());

        assertFloatEquals(89, ref.dec(10));
        assertFloatEquals(89, ref.get());

        assertFloatEquals(100, ref.dec(-11));
        assertFloatEquals(100, ref.get());
    }


    @Test
    public void testEquals() {
        FloatRef ref1 = new FloatRef(10);
        FloatRef ref2 = new FloatRef(10);
        FloatRef ref3 = new FloatRef(20);

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
        assertEquals(new Float(100).hashCode(), new FloatRef(100).hashCode());
        assertEquals(new Float(10).hashCode(), new FloatRef(10).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("FloatRef(value=10.0)", new FloatRef(10).toString());
    }

    @Test
    public void testAtomic() {
        FloatRef ref1 = new FloatRef(10);
        FloatRef ref2 = new FloatRef(20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertFloatEquals(10, ref1.get());
        assertFloatEquals(20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(FloatRef... refs) {
        for (FloatRef ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }
}
