package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLong_commutingIncLongTest {

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
    public void whenNoTransactionAndLocked_thenTooManyRetries() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);
        AlphaProgrammaticLongTranlocal committed = (AlphaProgrammaticLongTranlocal) ref.___load();

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        try {
            ref.commutingInc(10);
            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertSame(lockOwner, ref.___getLockOwner());
    }

}
