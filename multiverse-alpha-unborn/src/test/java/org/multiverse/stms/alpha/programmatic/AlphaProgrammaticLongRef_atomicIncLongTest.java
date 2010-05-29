package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.AbstractTransactionImpl;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_atomicIncLongTest {

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
    public void whenLocked_thenTooManyRetriesException() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 1);
        AlphaProgrammaticLongRefTranlocal committed = (AlphaProgrammaticLongRefTranlocal) ref.___load();

        Transaction lockOwner = new AbstractTransactionImpl();
        ref.___tryLock(lockOwner);


        long version = stm.getVersion();
        try {
            ref.atomicInc(10);
            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertSame(lockOwner, ref.___getLockOwner());
        assertSame(committed, ref.___load());
    }


}
