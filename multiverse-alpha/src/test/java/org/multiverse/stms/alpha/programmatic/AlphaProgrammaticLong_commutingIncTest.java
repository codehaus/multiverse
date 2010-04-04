package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

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
    @Ignore
    public void whenNoTransactionIsRunningAndNoCommits() {

    }

    @Test
    @Ignore
    public void whenTransactionRunning() {

    }

    @Test
    @Ignore
    public void whenTransactionRunning_andAlreadyOpenedForUpdate() {

    }

    @Test
    @Ignore
    public void whenTransactionRunning_andAlreadyOpenedForRead() {

    }
}
