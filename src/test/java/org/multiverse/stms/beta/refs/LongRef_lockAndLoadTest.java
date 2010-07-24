package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_lockAndLoadTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenFresh() {
        BetaTransaction lockOwner = stm.start();
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        Tranlocal tranlocal = ref.lockAndLoad(0, lockOwner);

        assertSame(committed, tranlocal);
        assertLocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertSame(lockOwner, ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
    }

    @Test
    public void whenAlreadyLockedByOtherTransaction() {
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        BetaTransaction lockingTx = new MonoBetaTransaction(stm);
        lockingTx.openForRead(ref, true, pool);

        BetaTransaction tx = stm.start();
        Tranlocal read = ref.lockAndLoad(0, tx);

        assertNotNull(read);
        assertTrue(read.locked);
        assertSame(committed, ref.unsafeLoad());
        assertLocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertSame(lockingTx, ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
    }

    @Test
    public void whenAlreadyLockedBySelf() {
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        BetaTransaction lockOwner = stm.start();
        Tranlocal read = lockOwner.openForRead(ref, true, pool);
        ref.tryLockAndCheckConflict(lockOwner, 1, read);

        Tranlocal tranlocal = ref.lockAndLoad(0, lockOwner);

        assertSame(committed, tranlocal);
        assertLocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertSame(lockOwner, ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
    }
}
