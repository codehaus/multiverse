package org.multiverse.transactional.primitives;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Stm;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalBooleanTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void constructorWithNoArg() {
        TransactionalBoolean ref = new TransactionalBoolean();
        assertFalse(ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        assertTrue(new TransactionalBoolean(true).get());
        assertFalse(new TransactionalBoolean(false).get());
    }

    @Test
    public void set() {
        TransactionalCharacter ref = new TransactionalCharacter((char) 10);
        long old = ref.set((char) 100);
        assertEquals((char) 10, old);
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testEquals() {
        TransactionalBoolean ref1 = new TransactionalBoolean(true);
        TransactionalBoolean ref2 = new TransactionalBoolean(true);
        TransactionalBoolean ref3 = new TransactionalBoolean(false);

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
        assertEquals(1, new TransactionalBoolean(true).hashCode());
        assertEquals(0, new TransactionalBoolean(false).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("TransactionalBoolean(value=true)", new TransactionalBoolean(true).toString());
        assertEquals("TransactionalBoolean(value=false)", new TransactionalBoolean(false).toString());
    }

    @Test
    public void testAtomic() {
        TransactionalBoolean ref1 = new TransactionalBoolean(true);
        TransactionalBoolean ref2 = new TransactionalBoolean(false);

        try {
            flipButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertTrue(ref1.get());
        assertFalse(ref2.get());
    }

    @TransactionalMethod
    public void flipButAbort(TransactionalBoolean... refs) {
        for (TransactionalBoolean ref : refs) {
            ref.set(!ref.get());
        }

        getThreadLocalTransaction().abort();
    }
}
