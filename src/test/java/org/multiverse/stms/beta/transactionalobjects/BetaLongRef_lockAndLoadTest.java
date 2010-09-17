package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_lockAndLoadTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    /*
    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenFresh() {
        BetaTransaction lockOwner = stm.startDefaultTransaction();
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        Tranlocal tranlocal = ref.___lockAndLoad(0, lockOwner, true);

        assertSame(committed, tranlocal);
        assertHasCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSame(lockOwner, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
    }

    @Test
    public void whenAlreadyLockedByOtherTransaction() {
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction lockingTx = new FatMonoBetaTransaction(stm);
        lockingTx.openForRead(ref, true);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = ref.___lockAndLoad(0, tx, true);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertSame(committed, ref.___unsafeLoad());
        assertHasCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSame(lockingTx, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
    }

    @Test
    public void whenAlreadyLockedBySelf() {
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction lockOwner = stm.startDefaultTransaction();
        Tranlocal read = lockOwner.openForRead(ref, true);
        ref.___tryLockAndCheckConflict(lockOwner, 1, read, true);

        Tranlocal tranlocal = ref.___lockAndLoad(0, lockOwner, true);

        assertSame(committed, tranlocal);
        assertHasCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSame(lockOwner, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
    } */
}
