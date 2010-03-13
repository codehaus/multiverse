package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.RetryError;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaRef_getOrAwaitTest {

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

        AlphaRef<String> ref = new AlphaRef<String>(oldRef);

        long version = stm.getVersion();

        String result = ref.getOrAwait();
        assertEquals(version, stm.getVersion());
        assertSame(oldRef, result);
    }

    @Test
    public void whenNull_retryError() {
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
