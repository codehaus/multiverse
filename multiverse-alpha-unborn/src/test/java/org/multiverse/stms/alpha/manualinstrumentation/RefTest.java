package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class RefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void after() {
        clearThreadLocalTransaction();
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();
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
        Transaction t = startUpdateTransaction();
        ref.set(b);
        ref.set(a);
        t.commit();
        assertEquals(startVersion, stm.getVersion());
    }

}
