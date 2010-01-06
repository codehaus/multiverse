package org.multiverse.datastructures.refs;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.RetryError;

/**
 * @author Peter Veentjer
 */
public class AbaRefTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
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
        AbaRef<String> ref = new AbaRef<String>(initialValue);

        long version = stm.getTime();

        Transaction t = stm.startUpdateTransaction("rollback");
        setThreadLocalTransaction(t);
        ref.set(newValue);
        t.abort();

        assertEquals(version, stm.getTime());
        assertSame(initialValue, ref.get());
    }

    // ==========================================

    @Test
    public void noArgConstruction() {
        long version = stm.getTime();

        AbaRef<String> ref = new AbaRef<String>();

        assertEquals(version + 1, stm.getTime());
        assertNull(ref.get());
    }

    @Test
    public void nullConstruction() {
        long version = stm.getTime();

        AbaRef<String> ref = new AbaRef<String>();

        assertEquals(version + 1, stm.getTime());
        assertEquals(null, ref.get());
    }

    @Test
    public void nonNullConstruction() {
        long version = stm.getTime();
        String s = "foo";
        AbaRef<String> ref = new AbaRef<String>(s);

        assertEquals(version + 1, stm.getTime());
        assertEquals(s, ref.get());
    }

    @Test
    public void testIsNull() {
        AbaRef<String> ref = new AbaRef<String>();

        long version = stm.getTime();
        assertTrue(ref.isNull());
        assertEquals(version, stm.getTime());

        ref.set("foo");

        version = stm.getTime();
        assertFalse(ref.isNull());
        assertEquals(version, stm.getTime());
    }

    @Test
    public void testSetFromNullToNull() {
        AbaRef<String> ref = new AbaRef<String>();

        long version = stm.getTime();
        String result = ref.set(null);
        assertNull(result);
        assertEquals(version, stm.getTime());
        assertNull(ref.get());
    }

    @Test
    public void testSetFromNullToNonNull() {
        AbaRef<String> ref = new AbaRef<String>();

        long version = stm.getTime();
        String newRef = "foo";
        String result = ref.set(newRef);
        assertNull(result);
        assertEquals(version + 1, stm.getTime());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetFromNonNullToNull() {
        String oldRef = "foo";
        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getTime();

        String result = ref.set(null);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getTime());
        assertNull(ref.get());
    }

    @Test
    public void testSetChangedReferenced() {
        String oldRef = "foo";

        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getTime();

        String newRef = "bar";
        String result = ref.set(newRef);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getTime());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetUnchangedReferences() {
        String oldRef = "foo";

        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getTime();

        String result = ref.set(oldRef);
        assertSame(oldRef, result);
        assertEquals(version, stm.getTime());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void testSetEqualIsNotUsedButReferenceEquality() {
        String oldRef = new String("foo");

        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getTime();

        String newRef = new String("foo");
        String result = ref.set(newRef);
        assertSame(oldRef, result);
        assertEquals(version + 1, stm.getTime());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetAndUnsetIsSeenAsChange() {
        String oldRef = "foo";
        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getTime();
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        String newRef = "bar";
        ref.set(newRef);
        ref.set(oldRef);
        t.commit();
        setThreadLocalTransaction(null);

        assertEquals(version + 1, stm.getTime());
        assertSame(oldRef, ref.get());
    }

    @Test
    public void getOrAwaitComletesIfRefNotNull() {
        String oldRef = "foo";

        AbaRef<String> ref = new AbaRef<String>(oldRef);

        long version = stm.getTime();

        String result = ref.getOrAwait();
        assertEquals(version, stm.getTime());
        assertSame(oldRef, result);
    }

    @Test
    public void getOrAwaitRetriesIfNull() {
        AbaRef<String> ref = new AbaRef<String>();

        long version = stm.getTime();

        //we start a transaction because we don't want to lift on the retry mechanism
        //of the transaction that else would be started on the getOrAwait method.
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        try {
            ref.getOrAwait();
            fail();
        } catch (RetryError retryError) {

        }
        assertEquals(version, stm.getTime());
    }
}
