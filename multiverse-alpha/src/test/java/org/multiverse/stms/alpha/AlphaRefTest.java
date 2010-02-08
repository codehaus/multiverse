package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.LoadUncommittedException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.RetryError;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class AlphaRefTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;
    private TransactionFactory readonlyTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        readonlyTxFactory = stm.getTransactionFactoryBuilder().setReadonly(true).build();
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
        AlphaRef<String> ref = new AlphaRef<String>(initialValue);

        long version = stm.getVersion();

        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        ref.set(newValue);
        tx.abort();

        assertEquals(version, stm.getVersion());
        assertSame(initialValue, ref.get());
    }

    // ============== createCommitted ==========================

    @Test
    public void createCommitted() {
        Transaction tx = updateTxFactory.start();
        long version = stm.getVersion();
        AlphaRef<String> ref = AlphaRef.createCommittedRef(stm, "foo");
        tx.abort();

        assertEquals(version + 1, stm.getVersion());
        assertEquals("foo", ref.get());
    }

    @Test
    public void createCommittedDoesntCareAboutAlreadyAvailableTransaction() {
        long version = stm.getVersion();

        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        AlphaRef<String> ref = AlphaRef.createCommittedRef(stm, null);
        tx.abort();

        assertTrue(ref.isNull());
        assertEquals(version + 1, stm.getVersion());

        ref.set("bar");
        assertEquals("bar", ref.get());
        assertFalse(ref.isNull());
    }

    // ============== constructor ==========================

    @Test
    public void noArgConstruction() {
        long version = stm.getVersion();

        AlphaRef<String> ref = new AlphaRef<String>();

        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void nullConstruction() {
        long version = stm.getVersion();

        AlphaRef<String> ref = new AlphaRef<String>();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(null, ref.get());
    }

    @Test
    public void nonNullConstruction() {
        long version = stm.getVersion();
        String s = "foo";
        AlphaRef<String> ref = new AlphaRef<String>(s);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(s, ref.get());
    }

    // ====================== isNull =================

    @Test
    public void testClearOnReadonlyRefFailsWithReadonlyException() {
        String value = "foo";
        AlphaRef<String> ref = new AlphaRef<String>(value);
        long version = stm.getVersion();

        Transaction tx = readonlyTxFactory.start();
        setThreadLocalTransaction(tx);

        try {
            ref.clear();
            fail();
        } catch (ReadonlyException ex) {
        }

        clearThreadLocalTransaction();
        assertEquals(version, stm.getVersion());
        assertSame(value, ref.get());
    }

    @Test
    public void testSetOnReadonlyRefFailsWithReadonlyException() {
        String value = "foo";
        AlphaRef<String> ref = new AlphaRef<String>(value);
        long version = stm.getVersion();

        Transaction tx = readonlyTxFactory.start();
        setThreadLocalTransaction(tx);

        try {
            ref.set(null);
            fail();
        } catch (ReadonlyException ex) {
        }

        clearThreadLocalTransaction();
        assertEquals(version, stm.getVersion());
        assertSame(value, ref.get());
    }

    @Test
    public void testIsNull() {
        AlphaRef<String> ref = new AlphaRef<String>();

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
        AlphaRef<String> ref = new AlphaRef<String>();

        long version = stm.getVersion();
        String result = ref.set(null);
        assertNull(result);
        assertEquals(version, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetFromNullToNonNull() {
        AlphaRef<String> ref = new AlphaRef<String>();

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
        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.set(null);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetChangedReferenced() {
        String oldRef = "foo";

        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

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

        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.set(oldRef);
        assertSame(oldRef, result);
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void testSetEqualIsNotUsedButReferenceEquality() {
        String oldRef = new String("foo");

        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

        long version = stm.getVersion();

        String newRef = new String("foo");
        String result = ref.set(newRef);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetAndUnsetIsNotSeenAsChange() {
        String oldRef = "foo";
        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

        long version = stm.getVersion();
        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        String newRef = "bar";
        ref.set(newRef);
        ref.set(oldRef);
        tx.commit();
        setThreadLocalTransaction(null);

        assertEquals(version, stm.getVersion());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void getOrAwaitCompletesIfRefNotNull() {
        String oldRef = "foo";

        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.getOrAwait();
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, result);
    }

    @Test
    public void getOrAwaitRetriesIfNull() {
        AlphaRef<String> ref = new AlphaRef<String>();

        long version = stm.getVersion();

        //we start a transaction because we don't want to lift on the retry mechanism
        //of the transaction that else would be started on the getOrAwait method.
        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        try {
            ref.getOrAwait();
            fail();
        } catch (RetryError retryError) {

        }
        assertEquals(version, stm.getVersion());
    }
}