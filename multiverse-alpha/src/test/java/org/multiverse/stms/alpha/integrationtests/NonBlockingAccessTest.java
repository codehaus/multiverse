package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.api.Transactions.startUpdateTransaction;

/**
 * @author Peter Veentjer
 */
public class NonBlockingAccessTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void writerDoesNotBlockReader() {
        TransactionalInteger ref = new TransactionalInteger(0);

        //begin the first transaction
        Transaction t1 = startUpdateTransaction(stm);
        setThreadLocalTransaction(t1);
        ref.inc();

        //begin the second transaction
        Transaction t2 = startUpdateTransaction(stm);
        setThreadLocalTransaction(t2);
        int value = ref.get();
        //commit the second transaction. This should succeed
        t2.commit();

        assertEquals(0, value);
    }

    @Test
    public void writerDoesNotBlockWriter() {
        TransactionalInteger ref = new TransactionalInteger(0);

        //begin the first transaction
        Transaction t1 = startUpdateTransaction(stm);
        setThreadLocalTransaction(t1);
        ref.inc();

        //begin the second transaction
        Transaction t2 = startUpdateTransaction(stm);
        setThreadLocalTransaction(t2);
        ref.inc();
        //commit the second transaction. This should succeed
        t2.commit();

        assertEquals(1, ref.get());
    }

    @Test
    public void readerDoesNotBlockWriter() {
        TransactionalInteger ref = new TransactionalInteger(0);

        //do the read in the first transaction but don't commit.
        Transaction t1 = startUpdateTransaction(stm);
        setThreadLocalTransaction(t1);
        ref.get();

        //do the write in the second transaction
        Transaction t2 = startUpdateTransaction(stm);
        setThreadLocalTransaction(t2);
        ref.inc();
        t2.commit();

        assertEquals(1, ref.get());
    }
}
