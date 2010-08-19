package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_loadTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenLockedByOther_thenLockedException() {
        LongRef ref = BetaStmUtils.createLongRef(stm);

        BetaTransaction other = stm.startDefaultTransaction();
        Tranlocal read1 = other.openForRead(ref, false, pool);
        ref.___tryLockAndCheckConflict(other, 1, read1);

        Tranlocal read = ref.___load(1);

        assertNotNull(read);
        assertTrue(read.isLocked);
    }

    @Test
    public void whenSuccess() {
        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        Tranlocal tranlocal = ref.___load(1);
        assertSame(committed, tranlocal);
    }
}
