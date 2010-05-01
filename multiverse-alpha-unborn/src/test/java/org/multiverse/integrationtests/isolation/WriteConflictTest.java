package org.multiverse.integrationtests.isolation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class WriteConflictTest {
    private Stm stm;
    private TransactionFactory txFactory;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = getGlobalStmInstance();
        txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void testLoadTimeWriteConflict() {
        Ref ref = new Ref();

        Transaction tx = txFactory.start();

        //conflicting update
        ref.inc();

        setThreadLocalTransaction(tx);
        try {
            ref.inc();
            fail();
        } catch (ReadConflict expected) {
        }

        clearThreadLocalTransaction();
        assertEquals(1, ref.get());
    }

    @Test
    public void testCommitTimeWriteConflict() {
        Ref ref = new Ref();

        Transaction tx = txFactory.start();
        setThreadLocalTransaction(tx);
        ref.inc();


        setThreadLocalTransaction(null);
        //conflicting update
        ref.inc();

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertEquals(1, ref.get());
    }
}
