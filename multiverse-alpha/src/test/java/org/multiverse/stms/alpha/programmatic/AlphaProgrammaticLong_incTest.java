package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.LockNotFreeWriteConflict;
import org.multiverse.api.exceptions.UncommittedReadConflict;
import org.multiverse.stms.AbstractTransactionImpl;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_incTest {
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
    public void whenNoTransactionOnThreadLocal() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        long version = stm.getVersion();
        ref.inc(1);

        assertEquals(11, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
        AlphaTranlocal committed = ref.___load();
        assertNotNull(committed);
        assertTrue(committed.isCommitted());
        assertNull(ref.___getLockOwner());
        assertEquals(version + 1, committed.getWriteVersion());
    }

    @Test
    public void whenNoTransactionAndNoChange() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ref.___load();

        long version = stm.getVersion();
        ref.inc(0);

        assertEquals(10, ref.atomicGet());
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertTrue(committed.isCommitted());
        assertNull(ref.___getLockOwner());
        assertEquals(version, committed.getWriteVersion());
    }

    @Test
    public void whenNoTransactionAndLocked() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ref.___load();

        Transaction lockOwner = new AbstractTransactionImpl();
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        try {
            ref.inc(10);
            fail();
        } catch (LockNotFreeWriteConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertSame(lockOwner, ref.___getLockOwner());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenNotTransactionAndCommittedBefore_thenUncommittedReadConflict() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        long version = stm.getVersion();
        try {
            ref.inc(10);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    @Ignore
    public void whenTransactionOnThreadLocal_thenItIsIgnored() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);

        Transaction tx = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setAutomaticReadTracking(true)
                .build()
                .start();

        setThreadLocalTransaction(tx);

        long version = stm.getVersion();
        long found = ref.inc(5);

        assertEquals(10, found);
        assertEquals(11, ref.atomicGet());
        assertEquals(version + 1, stm.getVersion());
        assertEquals(version + 1, ref.___load().getWriteVersion());
    }
}
