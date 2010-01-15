package org.multiverse.transactional.primitives;

import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalCharacterTest {

    @Test
    public void constructorWithNoArg() {
        TransactionalCharacter ref = new TransactionalCharacter();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        TransactionalCharacter ref = new TransactionalCharacter((char) 10);
        assertEquals((char) 10, ref.get());
    }

    @Test
    public void set() {
        TransactionalCharacter ref = new TransactionalCharacter((char) 10);
        long old = ref.set((char) 100);
        assertEquals((char) 10, old);
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testInc() {
        TransactionalCharacter ref = new TransactionalCharacter((char) 100);

        assertEquals((char) 101, ref.inc());
        assertEquals((char) 101, ref.get());

        assertEquals((char) 111, ref.inc((char) 10));
        assertEquals((char) 111, ref.get());

        assertEquals((char) 100, ref.inc((char) -11));
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testDec() {
        TransactionalCharacter ref = new TransactionalCharacter((char) 100);

        assertEquals((char) 99, ref.dec());
        assertEquals((char) 99, ref.get());

        assertEquals((char) 89, ref.dec((char) 10));
        assertEquals((char) 89, ref.get());

        assertEquals((char) 100, ref.dec((char) -11));
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testEquals() {
        TransactionalCharacter ref1 = new TransactionalCharacter((char) 10);
        TransactionalCharacter ref2 = new TransactionalCharacter((char) 10);
        TransactionalCharacter ref3 = new TransactionalCharacter((char) 20);

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
        TransactionalCharacter ref = new TransactionalCharacter((char) 10);
        assertEquals(10, ref.hashCode());

        ref.set((char) 200);
        assertEquals(200, ref.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("TransactionalCharacter(value=a)", new TransactionalCharacter('a').toString());
    }

    @Test
    public void testAtomic() {
        TransactionalCharacter ref1 = new TransactionalCharacter('a');
        TransactionalCharacter ref2 = new TransactionalCharacter('b');

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals('a', ref1.get());
        assertEquals('b', ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(TransactionalCharacter... refs) {
        for (TransactionalCharacter ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }


}
