package org.multiverse.transactional.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CharacterRefTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void constructorWithNoArg() {
        CharRef ref = new CharRef();
        assertEquals(0, ref.get());
    }

    @Test
    public void constructorWithSingleArg() {
        CharRef ref = new CharRef((char) 10);
        assertEquals((char) 10, ref.get());
    }

    @Test
    public void set() {
        CharRef ref = new CharRef((char) 10);
        long old = ref.set((char) 100);
        assertEquals((char) 10, old);
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testInc() {
        CharRef ref = new CharRef((char) 100);

        assertEquals((char) 101, ref.inc());
        assertEquals((char) 101, ref.get());

        assertEquals((char) 111, ref.inc((char) 10));
        assertEquals((char) 111, ref.get());

        assertEquals((char) 100, ref.inc((char) -11));
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testDec() {
        CharRef ref = new CharRef((char) 100);

        assertEquals((char) 99, ref.dec());
        assertEquals((char) 99, ref.get());

        assertEquals((char) 89, ref.dec((char) 10));
        assertEquals((char) 89, ref.get());

        assertEquals((char) 100, ref.dec((char) -11));
        assertEquals((char) 100, ref.get());
    }

    @Test
    public void testEquals() {
        CharRef ref1 = new CharRef((char) 10);
        CharRef ref2 = new CharRef((char) 10);
        CharRef ref3 = new CharRef((char) 20);

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
        CharRef ref = new CharRef((char) 10);
        assertEquals(10, ref.hashCode());

        ref.set((char) 200);
        assertEquals(200, ref.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("CharRef(value=a)", new CharRef('a').toString());
    }

    @Test
    public void testAtomic() {
        CharRef ref1 = new CharRef('a');
        CharRef ref2 = new CharRef('b');

        try {
            incButAbort(ref1, ref2);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals('a', ref1.get());
        assertEquals('b', ref2.get());
    }

    @TransactionalMethod
    public void incButAbort(CharRef... refs) {
        for (CharRef ref : refs) {
            ref.inc();
        }

        getThreadLocalTransaction().abort();
    }

    @Test
    public void awaitTest() {
        final CharRef ref = new CharRef('a');

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                ref.await('c');
            }
        };

        t.start();
        sleepMs(500);

        assertAlive(t);

        ref.set('b');
        sleepMs(500);

        assertAlive(t);

        ref.set('c');
        joinAll(t);
    }

}
