package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockNotFreeWriteConflict;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_commutingIncTest {
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
    public void whenNoTransactionIsRunning_andNoChange() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);
        AlphaProgrammaticLongTranlocal readonly = (AlphaProgrammaticLongTranlocal) ref.___load();

        long version = stm.getVersion();
        ref.commutingInc(0);

        assertEquals(version, stm.getVersion());
        assertSame(readonly, ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNoTransactionIsRunning_thenItIsExecutedAtomically() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        long version = stm.getVersion();
        ref.commutingInc(3);

        assertEquals(version + 1, stm.getVersion());
        AlphaProgrammaticLongTranlocal readonly = (AlphaProgrammaticLongTranlocal) ref.___load();
        assertNotNull(readonly);
        assertEquals(version + 1, readonly.getWriteVersion());
        assertEquals(13, ref.atomicGet());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNoTransactionIsRunningAndNoCommits() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        long version = stm.getVersion();
        try {
            ref.commutingInc(1);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNoTransactionAndLocked_thenLockNotFreeWriteConflict() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ref.___load();

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        try {
            ref.commutingInc(10);
            fail();
        } catch (LockNotFreeWriteConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertSame(lockOwner, ref.___getLockOwner());
    }

    @Test
    public void whenTransactionRunning() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();
        setThreadLocalTransaction(tx);

        long version = stm.getVersion();
        ref.commutingInc(10);
        tx.commit();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(11, ref.atomicGet());
    }
}
