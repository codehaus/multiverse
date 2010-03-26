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
public class AlphaProgrammaticReference_clearTest {

    private Stm stm;
    private TransactionFactory readonlyTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        readonlyTxFactory = stm.getTransactionFactoryBuilder().setReadonly(true).build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReferenceNotNull() {
        String value = "foo";
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(value);

        long version = stm.getVersion();
        String result = ref.clear();

        assertSame(value, result);
        assertEquals(version + 1, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void whenReferenceNull() {
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>();

        long version = stm.getVersion();
        ref.clear();

        assertEquals(version, stm.getVersion());
        assertNull(ref.get());
    }

    @Test
    public void whenTransactionReadonly_thenReadonlyException() {
        String value = "foo";
        AlphaProgrammaticReference<String> ref = new AlphaProgrammaticReference<String>(value);
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
}
