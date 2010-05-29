package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_setTest {
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

    @Test
    public void whenNoTransactionAvailable_thenCallExecutedAtomically() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        long version = stm.getVersion();
        ref.set(20);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(20, ref.atomicGet());
        //assertEquals(version + 1, ref.atomicGetVersion());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNoChange() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        AlphaProgrammaticLongRefTranlocal tranlocal = (AlphaProgrammaticLongRefTranlocal) ref.___load();

        long version = stm.getVersion();

        long found = ref.set(10);

        assertEquals(version, stm.getVersion());
        assertEquals(10, found);
        assertSame(tranlocal, ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        AlphaProgrammaticLongRef ref = AlphaProgrammaticLongRef.createUncommitted(stm);

        long version = stm.getVersion();
        try {
            ref.set(10);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenListenersExists_thenTheyAreNotified() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        Latch latch = new CheapLatch();
        ref.___registerRetryListener(latch, stm.getVersion() + 1);

        ref.set(20);

        assertNull(ref.___getListeners());
        assertTrue(latch.isOpen());
    }


    @Test
    public void whenTransactionAvailable_thenItLiftsOnThatTransaction() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ref.___load();

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .setReadTrackingEnabled(true)
                .build()
                .start();

        setThreadLocalTransaction(tx);

        long version = stm.getVersion();
        long found = ref.set(20);

        tx.abort();

        assertEquals(10, found);
        assertEquals(10, ref.atomicGet());
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertNull(ref.___getListeners());
    }
}
