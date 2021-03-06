package org.multiverse.stms.alpha.programmatic;

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
public class AlphaProgrammaticRef_getOrAwaitTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNotNull_thenValueReturned() {
        String oldRef = "foo";

        AlphaProgrammaticRef<String> ref = new AlphaProgrammaticRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.getOrAwait();
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, result);
    }

    @Test
    public void whenNull_retryError() {
        AlphaProgrammaticRef<String> ref = new AlphaProgrammaticRef<String>();

        long version = stm.getVersion();

        //we start a transaction because we don't want to lift on the retry mechanism
        //of the transaction that else would be started on the getOrAwait method.
        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        try {
            ref.getOrAwait();
            fail();
        } catch (Retry retry) {

        }
        assertEquals(version, stm.getVersion());
    }
}
