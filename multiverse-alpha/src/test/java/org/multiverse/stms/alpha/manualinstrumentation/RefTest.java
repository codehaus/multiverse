package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.Transactions.startUpdateTransaction;

/**
 * @author Peter Veentjer
 */
public class RefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @After
    public void after() {
        setThreadLocalTransaction(null);
    }

    public Transaction startTransaction() {
        Transaction t = startUpdateTransaction(stm);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testConstruction_empty() {
        Ref ref = new Ref();
        assertTrue(ref.isNull());
    }

    @Test
    public void testConstruction_nonNull() {
        String value = "foo";
        Ref<String> ref = new Ref<String>(value);
        assertSame(value, ref.get());
    }

    @Test
    public void testConstruction_null() {
        Ref<String> ref = new Ref<String>(null);
        assertNull(ref.get());
    }

    @Test
    public void clearEmpty() {
        Ref<String> ref = new Ref<String>();
        assertNull(ref.clear());
    }

    @Test
    public void clearNonEmpty() {
        String value = "foo";
        Ref<String> ref = new Ref<String>(value);
        assertSame(value, ref.clear());
    }

    @Test
    public void setNull() {
        Ref<String> ref = new Ref<String>();
        long startVersion = stm.getVersion();

        ref.set(null);

        assertEquals(startVersion, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void setNotNull() {
        Ref<String> ref = new Ref<String>();
        long startVersion = stm.getVersion();

        String value = "foo";
        ref.set(value);

        assertEquals(startVersion + 1, stm.getVersion());
        assertSame(value, ref.get());
    }

    @Test
    public void testAbaProblemIsNotDetected() {
        final String a = "A";
        final String b = "B";

        final Ref<String> ref = new Ref<String>(a);

        long startVersion = stm.getVersion();
        Transaction t = startTransaction();
        ref.set(b);
        ref.set(a);
        t.commit();
        assertEquals(startVersion, stm.getVersion());
    }

}
