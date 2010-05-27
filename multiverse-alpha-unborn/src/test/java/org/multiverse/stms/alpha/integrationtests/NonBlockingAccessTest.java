package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class NonBlockingAccessTest {

    private AlphaStm stm;
    private TransactionFactory<AlphaTransaction> updateTxFactory;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        updateTxFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void writerDoesNotBlockReader() {
        IntRef ref = new IntRef(0);

        //begin the first transaction
        Transaction t1 = updateTxFactory.start();
        setThreadLocalTransaction(t1);
        ref.inc();

        //begin the second transaction
        Transaction t2 = updateTxFactory.start();
        setThreadLocalTransaction(t2);
        int value = ref.get();
        //commit the second transaction. This should succeed
        t2.commit();

        assertEquals(0, value);
    }

    @Test
    public void writerDoesNotBlockWriter() {
        IntRef ref = new IntRef(0);

        //begin the first transaction
        Transaction t1 = updateTxFactory.start();
        setThreadLocalTransaction(t1);
        ref.inc();

        //begin the second transaction
        Transaction t2 = updateTxFactory.start();
        setThreadLocalTransaction(t2);
        ref.inc();
        //commit the second transaction. This should succeed
        t2.commit();

        assertEquals(1, ref.get());
    }

    @Test
    public void readerDoesNotBlockWriter() {
        IntRef ref = new IntRef(0);

        //do the read in the first transaction but don't commit.
        Transaction t1 = updateTxFactory.start();
        setThreadLocalTransaction(t1);
        ref.get();

        //do the write in the second transaction
        Transaction t2 = updateTxFactory.start();
        setThreadLocalTransaction(t2);
        ref.inc();
        t2.commit();

        assertEquals(1, ref.get());
    }
}
