package org.multiverse.transactional.refs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.Retry;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AbaRefTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
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
        AbaRef<String> ref = new AbaRef<String>(initialValue);

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

        AbaRef<String> ref = new AbaRef<String>();

        assertEquals(version, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void nullConstruction() {
        long version = stm.getVersion();

        AbaRef<String> ref = new AbaRef<String>();

        assertEquals(version, stm.getVersion());
        assertEquals(null, ref.get());
    }

    @Test
    public void nonNullConstruction() {
        long version = stm.getVersion();
        String s = "foo";
        AbaRef<String> ref = new AbaRef<String>(s);

        assertEquals(version, stm.getVersion());
        assertEquals(s, ref.get());
    }

    @Test
    public void testIsNull() {
        AbaRef<String> ref = new AbaRef<String>();

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
        AbaRef<String> ref = new AbaRef<String>();

        long version = stm.getVersion();
        String result = ref.set(null);
        assertNull(result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetFromNullToNonNull() {
        AbaRef<String> ref = new AbaRef<String>();

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
        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.set(null);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetChangedReferenced() {
        String oldRef = "foo";

        AbaRef<String> ref = new AbaRef<String>(oldRef);

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

        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.set(oldRef);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void testSetEqualIsNotUsedButReferenceEquality() {
        String oldRef = new String("foo");

        AbaRef<String> ref = new AbaRef<String>(oldRef);

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
        AbaRef<String> ref = new AbaRef<String>(oldRef);

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

        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.getOrAwait();
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, result);
    }

    @Test
    public void getOrAwaitRetriesIfNull() {
        AbaRef<String> ref = new AbaRef<String>();

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
