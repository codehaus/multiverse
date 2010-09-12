package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_loadTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenLockedByOther_thenLockedException() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        BetaTransaction other = stm.startDefaultTransaction();
        Tranlocal read1 = other.openForRead(ref, false);
        ref.___tryLockAndCheckConflict(other, 1, read1);

        Tranlocal read = ref.___load(1);

        assertNotNull(read);
        assertTrue(read.isLocked);
    }

    @Test
    public void whenSuccess() {
        BetaTransactionalObject ref = BetaStmUtils.newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        Tranlocal tranlocal = ref.___load(1);
        assertSame(committed, tranlocal);
    }
}
