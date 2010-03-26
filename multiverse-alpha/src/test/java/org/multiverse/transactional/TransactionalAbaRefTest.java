package org.multiverse.transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.Retry;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalAbaRefTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    // ================ rollback =============

    @Test
    public void rollback() {
        rollback(null, null);
        rollback(null, "foo");
        rollback("bar", "foo");
        rollback("bar", null);
    }

    public void rollback(String initialValue, String newValue) {
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(initialValue);

        long version = stm.getVersion();

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build().start();
        setThreadLocalTransaction(tx);
        ref.set(newValue);
        tx.abort();

        assertEquals(version, stm.getVersion());
        assertSame(initialValue, ref.get());
    }

    // ==========================================

    @Test
    public void noArgConstruction() {
        long version = stm.getVersion();

        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>();

        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void nullConstruction() {
        long version = stm.getVersion();

        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(null, ref.get());
    }

    @Test
    public void nonNullConstruction() {
        long version = stm.getVersion();
        String s = "foo";
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(s);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(s, ref.get());
    }

    @Test
    public void testIsNull() {
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>();

        long version = stm.getVersion();
        assertTrue(ref.isNull());
        assertEquals(version, stm.getVersion());

        ref.set("foo");

        version = stm.getVersion();
        assertFalse(ref.isNull());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void testSetFromNullToNull() {
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>();

        long version = stm.getVersion();
        String result = ref.set(null);
        assertNull(result);
        assertEquals(version, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetFromNullToNonNull() {
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>();

        long version = stm.getVersion();
        String newRef = "foo";
        String result = ref.set(newRef);
        assertNull(result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetFromNonNullToNull() {
        String oldRef = "foo";
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.set(null);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetChangedReferenced() {
        String oldRef = "foo";

        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(oldRef);

        long version = stm.getVersion();

        String newRef = "bar";
        String result = ref.set(newRef);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetUnchangedReferences() {
        String oldRef = "foo";

        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.set(oldRef);
        assertSame(oldRef, result);
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void testSetEqualIsNotUsedButReferenceEquality() {
        String oldRef = new String("foo");

        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(oldRef);

        long version = stm.getVersion();

        String newRef = new String("foo");
        String result = ref.set(newRef);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetAndUnsetIsSeenAsChange() {
        String oldRef = "foo";
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(oldRef);

        long version = stm.getVersion();
        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        String newRef = "bar";
        ref.set(newRef);
        ref.set(oldRef);
        tx.commit();
        setThreadLocalTransaction(null);

        assertEquals(version + 1, stm.getVersion());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void getOrAwaitComletesIfRefNotNull() {
        String oldRef = "foo";

        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.getOrAwait();
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, result);
    }

    @Test
    public void getOrAwaitRetriesIfNull() {
        TransactionalAbaReference<String> ref = new TransactionalAbaReference<String>();

        long version = stm.getVersion();

        //we start a transaction because we don't want to lift on the retry mechanism
        //of the transaction that else would be started on the getOrAwait method.
        Transaction tx = stm.getTransactionFactoryBuilder().build().start();
        setThreadLocalTransaction(tx);
        try {
            ref.getOrAwait();
            fail();
        } catch (Retry retry) {

        }
        assertEquals(version, stm.getVersion());
    }
}
