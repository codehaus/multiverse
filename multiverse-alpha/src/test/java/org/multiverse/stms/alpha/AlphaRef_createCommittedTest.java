package org.multiverse.stms.alpha;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaRef_createCommittedTest {

    private Stm stm;
    private TransactionFactory updateTxFactory;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder().build();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void createCommitted() {
        Transaction tx = updateTxFactory.start();
        long version = stm.getVersion();
        AlphaRef<String> ref = AlphaRef.createCommittedRef(stm, "foo");
        tx.abort();

        assertEquals(version + 1, stm.getVersion());
        assertEquals("foo", ref.get());
    }

    @Test
    public void createCommittedDoesntCareAboutAlreadyAvailableTransaction() {
        long version = stm.getVersion();

        Transaction tx = updateTxFactory.start();
        setThreadLocalTransaction(tx);
        AlphaRef<String> ref = AlphaRef.createCommittedRef(stm, null);
        tx.abort();

        assertTrue(ref.isNull());
        assertEquals(version + 1, stm.getVersion());

        ref.set("bar");
        assertEquals("bar", ref.get());
        assertFalse(ref.isNull());
    }
}
