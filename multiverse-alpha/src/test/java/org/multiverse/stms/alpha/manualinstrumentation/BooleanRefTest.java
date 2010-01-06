package org.multiverse.stms.alpha.manualinstrumentation;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class BooleanRefTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void atomicConstruction() {
        BooleanRef v = new BooleanRef(true);
        assertEquals(true, v.get());
    }

    @Test
    public void atomicModificiation() {
        long startVersion = stm.getTime();

        //create
        BooleanRef v = new BooleanRef(true);
        assertNull(getThreadLocalTransaction());

        //modify
        v.set(false);
        assertNull(getThreadLocalTransaction());

        //read
        assertFalse(v.get());
        assertNull(getThreadLocalTransaction());

        //since 2 update-transactions have been executed, we know that the clock version should be
        //increased by 2.
        assertEquals(startVersion + 2, stm.getTime());
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void testUpdateOnAlreadyAvailableTransaction() {
        long startVersion = stm.getTime();
        Transaction t = startUpdateTransaction();

        BooleanRef v = new BooleanRef(true);
        v.set(false);
        assertFalse(v.get());

        t.commit();

        //since everything is done under a single transaction, the clock version shouls be increased by 1
        assertEquals(startVersion + 1, stm.getTime());
        assertNotNull(getThreadLocalTransaction());
    }
}
