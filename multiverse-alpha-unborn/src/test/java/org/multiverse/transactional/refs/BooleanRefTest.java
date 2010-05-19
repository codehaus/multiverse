package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class BooleanRefTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void constructorWithNoArg() {
        BooleanRef ref = new BooleanRef();
        assertFalse(ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        assertTrue(new BooleanRef(true).get());
        assertFalse(new BooleanRef(false).get());
    }

    @Test
    public void set() {
        CharRef ref = new CharRef((char) 10);
        long old = ref.set((char) 100);
        assertEquals((char) 10, old);
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testEquals() {
        BooleanRef ref1 = new BooleanRef(true);
        BooleanRef ref2 = new BooleanRef(true);
        BooleanRef ref3 = new BooleanRef(false);

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
        assertEquals(1, new BooleanRef(true).hashCode());
        assertEquals(0, new BooleanRef(false).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("BooleanRef(value=true)", new BooleanRef(true).toString());
        assertEquals("BooleanRef(value=false)", new BooleanRef(false).toString());
    }

    @Test
    public void testAtomic() {
        BooleanRef ref1 = new BooleanRef(true);
        BooleanRef ref2 = new BooleanRef(false);

        try {
            flipButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertTrue(ref1.get());
        assertFalse(ref2.get());
    }

    @TransactionalMethod
    public void flipButAbort(BooleanRef... refs) {
        for (BooleanRef ref : refs) {
            ref.set(!ref.get());
        }

        getThreadLocalTransaction().abort();
    }
}
