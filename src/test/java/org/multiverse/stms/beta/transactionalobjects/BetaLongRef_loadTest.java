package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_loadTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
        pool = new BetaObjectPool();
    }

    /*
    @Test
    public void whenEnsuredByOther_thenReadAllowed() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        BetaTranlocal read = ref.___load(1, null, LOCKMODE_NONE, pool);

        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenPrivatizedByOther_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTranlocal read = ref.___load(1, null, LOCKMODE_NONE, pool);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenFirstTimeReadOnUpdateBiased() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaLongRefTranlocal tranlocal = ref.___load(1, null, LOCKMODE_NONE, pool);

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenSecondTimeReadOnUpdateBiased() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        ref.___load(1, null, LOCKMODE_NONE, pool);
        BetaLongRefTranlocal tranlocal = ref.___load(1, null, LOCKMODE_NONE, pool);

        assertEquals(version, tranlocal.version);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertSurplus(2, ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = makeReadBiased(newLongRef(stm, 10));
        long version = ref.getVersion();

        BetaLongRefTranlocal tranlocal = ref.___load(1, null, LOCKMODE_NONE, pool);

        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommitted);
        assertFalse(tranlocal.isDirty());
        assertFalse(tranlocal.isCommuting);
        assertEquals(version, tranlocal.version);
        assertEquals(10, tranlocal.value);
        assertTrue(tranlocal.hasDepartObligation);
        assertFalse(tranlocal.isLockOwner);
    }

    @Test
    @Ignore
    public void whenJustBecomeReadBiased() {

    }

    @Test
    public void whenEnsuredByOtherAndEnsure_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal read = ref.___load(1, tx, LOCKMODE_UPDATE, pool);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenEnsuredByOtherAndPrivatized_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal read = ref.___load(1, tx, LOCKMODE_COMMIT, pool);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenPrivatizedByOtherAndEnsure_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal read = ref.___load(1, tx, LOCKMODE_UPDATE, pool);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenPrivatizedByOtherAndPrivatize_thenReturnLocked() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal read = ref.___load(1, tx, LOCKMODE_COMMIT, pool);

        assertNotNull(read);
        assertTrue(read.isLocked);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    @Ignore
    public void whenReadByOther() {

    }

    @Test
    @Ignore
    public void whenPendingUpdateByOther() {

    }                   */
}
