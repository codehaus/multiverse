package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class RefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void after() {
        setThreadLocalTransaction(null);
    }

    public Transaction startTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
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
        long startVersion = stm.getTime();

        ref.set(null);

        assertEquals(startVersion, stm.getTime());
        assertNull(ref.get());
    }

    @Test
    public void setNotNull() {
        Ref<String> ref = new Ref<String>();
        long startVersion = stm.getTime();

        String value = "foo";
        ref.set(value);

        assertEquals(startVersion + 1, stm.getTime());
        assertSame(value, ref.get());
    }

    @Test
    public void testAbaProblemIsNotDetected() {
        final String a = "A";
        final String b = "B";

        final Ref<String> ref = new Ref<String>(a);

        long startVersion = stm.getTime();
        Transaction t = startTransaction();
        ref.set(b);
        ref.set(a);
        t.commit();
        assertEquals(startVersion, stm.getTime());
    }

}
