package org.multiverse.transactional.arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalReferenceArray_atomicSetTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.atomicSet(10, "foo");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        array.atomicSet(-1, "foo");
    }

    @Test
    public void whenNoValueChange() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        String value = "foo";

        array.set(0, value);

        long version = stm.getVersion();
        String found = array.atomicSet(0, value);
        assertSame(value, found);
        assertEquals(version, stm.getVersion());
        assertSame(value, array.atomicGet(0));
    }

    @Test
    public void whenValueChanged() {
        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        String old = "foo";

        array.set(0, old);

        long version = stm.getVersion();
        String update = "bar";
        String found = array.atomicSet(0, update);
        assertSame(old, found);
        assertEquals(version + 1, stm.getVersion());
        assertSame(update, array.atomicGet(0));
    }

    @Test
    public void whenTransactionActiveThenIgnored() {
        Transaction tx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build()
                .start();
        setThreadLocalTransaction(tx);

        TransactionalReferenceArray<String> array = new TransactionalReferenceArray<String>(10);
        String old = "foo";

        array.atomicSet(0, old);

        long version = stm.getVersion();
        String update = "bar";
        String found = array.atomicSet(0, update);
        assertSame(old, found);
        assertEquals(version + 1, stm.getVersion());
        assertSame(update, array.atomicGet(0));
    }
}
