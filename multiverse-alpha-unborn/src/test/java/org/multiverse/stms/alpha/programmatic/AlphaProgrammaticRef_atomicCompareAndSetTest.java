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
public class AlphaProgrammaticRef_atomicCompareAndSetTest {
    private AlphaStm stm;

    private static final Long ONE = 1L;
    private static final Long TWO = 2L;
    private static final Long THREE = 3L;

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
        AlphaProgrammaticRef<Long> ref = AlphaProgrammaticRef.createUncommitted();

        long version = stm.getVersion();
        try {
            ref.atomicCompareAndSet(ONE, TWO);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenValueMatches() {
        AlphaProgrammaticRef<Long> ref = new AlphaProgrammaticRef<Long>(stm, ONE);

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(ONE, TWO);

        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(TWO, ref.get());
        assertNull(ref.___getLockOwner());

        AlphaProgrammaticRefTranlocal current = (AlphaProgrammaticRefTranlocal) ref.___load();
        assertNotNull(current);
        assertTrue(current.isCommitted());
        assertEquals(TWO, current.value);
        assertEquals(version + 1, current.___writeVersion);
    }

    @Test
    public void whenLocked_thenTooManyRetriesException() {
        AlphaProgrammaticRef<Long> ref = new AlphaProgrammaticRef<Long>(stm, ONE);
        AlphaProgrammaticRefTranlocal committed = (AlphaProgrammaticRefTranlocal) ref.___load();

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(ONE, TWO);

        assertFalse(result);
        assertSame(lockOwner, ref.___getLockOwner());
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenNoChange() {
        AlphaProgrammaticRef<Long> ref = new AlphaProgrammaticRef<Long>(stm, ONE);
        AlphaProgrammaticRefTranlocal committed = (AlphaProgrammaticRefTranlocal) ref.___load();
        long version = stm.getVersion();
        boolean success = ref.atomicCompareAndSet(ONE, ONE);

        assertTrue(success);
        assertEquals(version, stm.getVersion());
        assertEquals(ONE, ref.atomicGet());
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenChangeThenListenersNotified() {
        AlphaProgrammaticRef<Long> ref = new AlphaProgrammaticRef<Long>(stm, ONE);
        Latch latch = new CheapLatch();
        ref.___registerRetryListener(latch, stm.getVersion() + 1);

        ref.atomicCompareAndSet(ONE, TWO);

        assertNull(ref.___getListeners());
        assertTrue(latch.isOpen());
    }

    @Test
    public void whenValueNotMatches() {
        AlphaProgrammaticRef ref = new AlphaProgrammaticRef(stm, ONE);
        AlphaProgrammaticRefTranlocal readonly = (AlphaProgrammaticRefTranlocal) ref.___load();

        long version = stm.getVersion();
        boolean result = ref.atomicCompareAndSet(TWO, THREE);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals(ONE, ref.get());
        assertNull(ref.___getLockOwner());
        assertSame(readonly, ref.___load());
    }
}
