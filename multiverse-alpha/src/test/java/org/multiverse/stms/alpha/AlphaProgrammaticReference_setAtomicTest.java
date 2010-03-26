package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticReference_setAtomicTest {
    private Stm stm;
    private TransactionFactory updateTxFactory;
    private TransactionFactory readonlyTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullAndNewValueNull() {
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>();

        long version = stm.getVersion();
        String result = ref.setAtomic(null);

        assertNull(result);
        assertEquals(version, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void whenNotNullAndNewValueNull() {
        String oldValue = "old";
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>("old");

        long version = stm.getVersion();
        String newValue = "foo";
        String result = ref.setAtomic(newValue);

        assertSame(oldValue, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newValue, ref.get());
    }

    @Test
    public void whenNotNullAndValueNull() {
        String oldValue = "foo";
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(oldValue);

        long version = stm.getVersion();
        String result = ref.setAtomic(null);

        assertSame(oldValue, result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void testSetChangedReferenced() {
        String oldValue = "foo";

        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(oldValue);

        long version = stm.getVersion();

        String newRef = "bar";
        String result = ref.setAtomic(newRef);
        assertSame(oldValue, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newRef, ref.get());
    }

    @Test
    public void testSetUnchangedReferences() {
        String oldValue = "foo";

        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(oldValue);

        long version = stm.getVersion();

        String result = ref.setAtomic(oldValue);
        assertSame(oldValue, result);
        assertEquals(version, stm.getVersion());
        assertSame(oldValue, ref.get());
    }

    @Test
    public void testSetEqualIsNotUsedButReferenceEquality() {
        String oldValue = new String("foo");

        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(oldValue);

        long version = stm.getVersion();

        String newValue = new String("foo");
        String result = ref.set(newValue);
        assertSame(oldValue, result);
        assertEquals(version + 1, stm.getVersion());
        assertSame(newValue, ref.get());
    }

    @Test
    public void testSetAndUnsetIsNotSeenAsChange() {
        String oldValue = "foo";
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(oldValue);

        long version = stm.getVersion();
        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        String newRef = "bar";
        ref.set(newRef);
        ref.set(oldValue);
        tx.commit();
        setThreadLocalTransaction(null);

        assertEquals(version, stm.getVersion());
        assertSame(oldValue, ref.get());
    }
}
