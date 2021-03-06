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
public class AlphaProgrammaticLongRef_atomicSetTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        long version = stm.getVersion();
        long found = ref.atomicSet(20);

        assertEquals(10, found);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(20, ref.atomicGet());
        assertNull(ref.___getLockOwner());

        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ref.___load();
        assertNotNull(committed);
        assertTrue(committed.isCommitted());
        assertEquals(version + 1, committed.getWriteVersion());
        assertEquals(20, committed.value);
    }


    @Test
    public void whenNoChange() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ref.___load();
        long version = stm.getVersion();
        long found = ref.atomicSet(10);

        assertEquals(10, found);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.atomicGet());
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenListenersExistsTheyAreNotified() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        Latch latch = new CheapLatch();
        ref.___registerRetryListener(latch, stm.getVersion() + 1);

        ref.atomicSet(20);

        assertNull(ref.___getListeners());
        assertTrue(latch.isOpen());
    }

    @Test
    public void whenNotCommittedBefore() {
        AlphaProgrammaticLongRef ref = AlphaProgrammaticLongRef.createUncommitted(stm);

        long version = stm.getVersion();
        try {
            ref.atomicSet(10);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenTransactionAvailable_thenItsIgnored() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(true)
                .build()
                .start();

        setThreadLocalTransaction(tx);

        long version = stm.getVersion();
        long found = ref.atomicSet(20);

        assertEquals(10, found);
        assertEquals(20, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
        assertEquals(version + 1, ref.___load().getWriteVersion());
    }
}
