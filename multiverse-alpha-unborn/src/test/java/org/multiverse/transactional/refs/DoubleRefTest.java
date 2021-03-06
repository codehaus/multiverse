package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static java.lang.Double.doubleToLongBits;
import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class DoubleRefTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void constructorWithNoArg() {
        DoubleRef ref = new DoubleRef();
        assertDoubleEquals(0, ref.get());
    }

    public void assertDoubleEquals(double expected, double found) {
        assertEquals(doubleToLongBits(expected), doubleToLongBits(found));
    }

    @Test
    public void constructorWithSingleArg() {
        DoubleRef ref = new DoubleRef(10);
        assertDoubleEquals(10, ref.get());
    }

    @Test
    public void set() {
        DoubleRef ref = new DoubleRef();
        double old = ref.set(100);
        assertDoubleEquals(0, old);
        assertDoubleEquals(100, ref.get());
    }

    @Test
    public void testInc() {
        DoubleRef ref = new DoubleRef(100);

        assertDoubleEquals(101, ref.inc());
        assertDoubleEquals(101, ref.get());

        assertDoubleEquals(111, ref.inc(10));
        assertDoubleEquals(111, ref.get());

        assertDoubleEquals(100, ref.inc(-11));
        assertDoubleEquals(100, ref.get());
    }

    @Test
    public void testDec() {
        DoubleRef ref = new DoubleRef(100);

        assertDoubleEquals(99, ref.dec());
        assertDoubleEquals(99, ref.get());

        assertDoubleEquals(89, ref.dec(10));
        assertDoubleEquals(89, ref.get());

        assertDoubleEquals(100, ref.dec(-11));
        assertDoubleEquals(100, ref.get());
    }


    @Test
    public void testEquals() {
        DoubleRef ref1 = new DoubleRef(10);
        DoubleRef ref2 = new DoubleRef(10);
        DoubleRef ref3 = new DoubleRef(20);

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
        assertEquals(new Double(100).hashCode(), new DoubleRef(100).hashCode());
        assertEquals(new Double(10).hashCode(), new DoubleRef(10).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("DoubleRef(value=10.0)", new DoubleRef(10).toString());
    }

    @Test
    public void testAtomic() {
        DoubleRef ref1 = new DoubleRef(10);
        DoubleRef ref2 = new DoubleRef(20);

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertDoubleEquals(10, ref1.get());
        assertDoubleEquals(20, ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(DoubleRef... refs) {
        for (DoubleRef ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }
}
