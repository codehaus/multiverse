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
import static org.mockito.Mockito.mock;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_atomicCompareAndSetTest {
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
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        AlphaProgrammaticLongRef ref = AlphaProgrammaticLongRef.createUncommitted(stm);

        long version = stm.getVersion();
        try {
            ref.atomicCompareAndSet(10, 20);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenValueMatches() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(1, 5);

        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(5, ref.get());
        assertNull(ref.___getLockOwner());

        AlphaProgrammaticLongRefTranlocal current = (AlphaProgrammaticLongRefTranlocal) ref.___load();
        assertNotNull(current);
        assertTrue(current.isCommitted());
        assertEquals(5, current.value);
        assertEquals(version + 1, current.___writeVersion);
    }

    @Test
    public void whenLocked_thenFalse() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ref.___load();

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(10, 20);

        assertFalse(result);
        assertSame(lockOwner, ref.___getLockOwner());
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenNoChange() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ref.___load();
        long version = stm.getVersion();
        boolean success = ref.atomicCompareAndSet(10, 10);

        assertTrue(success);
        assertEquals(version, stm.getVersion());
        assertEquals(10, ref.atomicGet());
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenChangeThenListenersNotified() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 10);
        Latch latch = new CheapLatch();
        ref.___registerRetryListener(latch, stm.getVersion() + 1);

        ref.atomicCompareAndSet(10, 20);

        assertNull(ref.___getListeners());
        assertTrue(latch.isOpen());
    }

    @Test
    public void whenValueNotMatches() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);
        AlphaProgrammaticLongRefTranlocal readonly = (AlphaProgrammaticLongRefTranlocal) ref.___load();

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(2, 5);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.get());
        assertNull(ref.___getLockOwner());
        assertSame(readonly, ref.___load());
    }
}
