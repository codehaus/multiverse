package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_loadTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenLockedByOther_thenLockedException() {
        LongRef ref = StmUtils.createLongRef(stm);

        BetaTransaction other = stm.start();
        Tranlocal read1 = other.openForRead(ref, false, pool);
        ref.tryLockAndCheckConflict(other, 1, read1);

        Tranlocal read = ref.load(1);

        assertNotNull(read);
        assertTrue(read.locked);
    }

    @Test
    public void whenSuccess() {
        BetaTransactionalObject ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        Tranlocal tranlocal = ref.load(1);
        assertSame(committed, tranlocal);
    }
}
