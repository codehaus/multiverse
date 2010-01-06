package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Transaction;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class NonBlockingAccessTest {


    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    @Test
    public void writerDoesNotBlockReader() {
        IntRef ref = new IntRef(0);

        //begin the first transaction
        AlphaTransaction t1 = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t1);
        ref.inc();

        //begin the second transaction
        AlphaTransaction t2 = stm.startUpdateTransaction(null);
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
        AlphaTransaction t1 = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t1);
        ref.inc();

        //begin the second transaction
        AlphaTransaction t2 = stm.startUpdateTransaction(null);
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
        Transaction t1 = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t1);
        ref.get();

        //do the write in the second transaction
        Transaction t2 = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t2);
        ref.inc();
        t2.commit();

        assertEquals(1, ref.get());
    }
}
