package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_getTest {

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
    public void whenNoTransactionRunning() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ref.___load();

        long version = stm.getVersion();
        long result = ref.get();

        assertEquals(10, result);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNoTransactionRunningAndUncommitted_() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        long version = stm.getVersion();
        long result = ref.get();

        assertEquals(0, result);
        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
    }

    @Test
    public void whenNoTransactionRunningAndLocked() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 10);
        AlphaTranlocal committed = ref.___load();

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        long result = ref.get();

        assertEquals(version, stm.getVersion());
        assertEquals(10, result);
        assertSame(committed, ref.___load());
        assertSame(lockOwner, ref.___getLockOwner());
    }

    @Test
    @Ignore
    public void whenTransactionAvailable() {

    }
}
