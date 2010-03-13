package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.ReadonlyException;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaRef_setTest {
    private Stm stm;
    private TransactionFactory updateTxFactory;
    private TransactionFactory readonlyTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        readonlyTxFactory = stm.getTransactionFactoryBuilder().setReadonly(true).build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
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

}
